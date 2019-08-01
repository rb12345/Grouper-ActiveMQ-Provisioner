#!/usr/bin/perl -w

#Copyright 2014 Carnegie Mellon University
#All rights reserved.

#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

#1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

#2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

#3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

use JSON;
use Log::Log4perl;
use FindBin;
use strict;

use CMU::LDAP::AD;
use CMU::LDAP::389;
use CMU::Util;
use CMU::ActiveMQ;
use CMU::CFG;
use CMU::LDAPFactory;
use CMU::Stats;

my $mesg;
my $log;
my $activemq;
my $ldap;
my $stats_initialized = 0;
my $done              = 0;
my $data;
my $frame;
my $nextframe;
my $batchsize = 100;

CMU::CFG::readConfig('configuration.pl');
Log::Log4perl::init( $CMU::CFG::_CFG{'log'}{'file'} );
$log = Log::Log4perl->get_logger();

$batchsize = $CMU::CFG::_CFG{'batchsize'};

while (1) {
	eval {
		$activemq = CMU::ActiveMQ->new();
		if ( defined $activemq ) {
			$frame = $activemq->getStomp()->receive_frame( { timeout => 30 } );
			while ($frame) {
				$mesg      = $frame->body;
				$nextframe =
				  $activemq->getStomp()->receive_frame( { timeout => 0.5 } );
				if ( defined $mesg ) {
					$log->debug("Received ActiveMQ message: $mesg");
					$data = JSON::decode_json($mesg);

					if ( !$stats_initialized ) {
						CMU::Stats::initializeStats();
						CMU::Stats::updateStat( "start_time", time() );
						$stats_initialized = 1;
					}

					CMU::Stats::incrementStat("amq_num_messages");
					if ( defined $data->{"memberList"} ) {
						CMU::Stats::addStat( "amq_num_users",
							scalar @{ $data->{"memberList"} } );
					}
					elsif ( defined $data->{"memberId"} ) {
						CMU::Stats::incrementStat("amq_num_users");
					}

					$ldap =
					  CMU::LDAPFactory::getInstance(
						$CMU::CFG::_CFG{'ldap'}{'env'}, $data );

					if ( defined $ldap ) {

						if ( $data->{"operation"} eq "fullSync" ) {
							$activemq->processMessageFullSync( $ldap, $data );
							$activemq->getStomp()->ack( { frame => $frame } );
							$log->debug(
"Successfully processed  ActiveMQ message: $mesg"
							);
						}elsif ( $data->{"operation"} eq "fullSyncPrivilege" ) {
							$activemq->processMessageFullSyncPrivilege( $ldap, $data );
							$activemq->getStomp()->ack( { frame => $frame } );
							$log->debug(
"Successfully processed  ActiveMQ message: $mesg"
							);
						}
						elsif ( $data->{"operation"} eq "fullSyncIsMemberOf" ) {
							$activemq->processMessageFullSyncIsMemberOf( $ldap,
								$data );
								$activemq->getStomp()->ack( { frame => $frame } );
							$log->debug(
"Successfully processed  ActiveMQ message: $mesg"
							);
						}
						elsif ($data->{"operation"} ne "addMember"
							&& $data->{"operation"} ne "removeMember" )
						{
							$activemq->processMessageChangeLog( $ldap, $data );
							$activemq->getStomp()->ack( { frame => $frame } );
							$log->debug(
"Successfully processed  ActiveMQ message: $mesg"
							);
						}
						else {
							$done =
							  $activemq->addMessageChangeLogToBatch( $ldap,
								$frame, $nextframe );

							my @unacked_frames = $activemq->getUnAckedFrames();

							if ( $done || $#unacked_frames == $batchsize ) {
								$activemq->processMessageChangeLogBatch($ldap);
								
								foreach my $unacked_frame (@unacked_frames){
    								$activemq->getStomp()
								  ->ack( { frame => $unacked_frame } );
								  $mesg = $unacked_frame->body;
								$log->debug(
									"Successfully processed  ActiveMQ message: "
									  . $mesg );
								}
								

								$done = 0;
								$activemq->resetChangeLogBatch();
							}
						}
						$frame = $nextframe;
					}
				}
			}
			if ($stats_initialized) {
				$stats_initialized = 0;
				CMU::Stats::updateStat( "end_time", time() );
				CMU::Stats::writeStats();
			}

			if ( defined $ldap ) {
				$ldap->getCache()->purge();
				$ldap->disconnect();
			}
		}

		if ($@) {
			my @unacked_frames = $activemq->getUnAckedFrames();
			if (@unacked_frames) {
				foreach (@unacked_frames) {
					$mesg = $_->body;
					$log->debug(
						"Couldn't process ActiveMQ message: $mesg .. Retrying");
				}
			}
			else {
				$log->debug(
					"Couldn't process ActiveMQ message: $mesg .. Retrying");
			}

			if ( defined $activemq ) {
				$activemq->disconnect();
			}

			if ( defined $ldap ) {
				$ldap->disconnect();
			}
			sleep 30;
		}
	};
}

