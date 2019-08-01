#Copyright 2014 Carnegie Mellon University
#All rights reserved.

#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

#1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

#2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

#3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package CMU::Util;
use Net::LDAP::Util qw(ldap_explode_dn);
use Data::Dumper;
use Log::Log4perl;
use strict;
use warnings;

my $log = Log::Log4perl->get_logger();

# get items from array @a that are not in array @b
sub arrayMinus {
	$log->debug("Calling  CMU::Util::arrayMinus(arrayA, arrayB)");
	my %e = map { $_ => undef } @{ $_[1] };
	return grep( !exists( $e{$_} ), @{ $_[0] } );
}

sub hashMinus {
	my ( $h1, $h2 ) = @_;
	$log->debug("Calling  CMU::Util::arrayMinus(hashA, hashB)");
	my $rh = {};

	foreach my $k ( keys %{$h1} ) {
		if ( ( !defined $h2->{$k} ) || ( $h1->{$k} ne $h2->{$k} ) ) {
			$rh->{$k} = $h1->{$k};
		}
	}
	return $rh;
}

sub covertMemberDNListToMembersUidList($ @) {
    my ( $target, @dn ) = @_;
    $log->debug(
        "Calling  CMU::Util::covertMemberDNListToMembersUidList($target, dn)");

    my @uid = ();
    CMU::CFG::readConfig('configuration.pl');

    foreach my $member_dn (@dn) {
        if ( $target eq "389" ) {
            if ( defined ldap_explode_dn($member_dn)->[0]{UID}
                 && length( ldap_explode_dn($member_dn)->[0]{UID} ) )
            {
                push( @uid, lc( ldap_explode_dn($member_dn)->[0]{UID} ) );
            }
            else {
                push( @uid, ldap_explode_dn($member_dn)->[0]{CN} );
            }
        }
        else {
            my $syncou = $CMU::CFG::_CFG{'AD'}{'syncou'};
            my $pos = index( lc($member_dn), lc($syncou) );
            if ( $pos >= 0 ) {
                $member_dn = substr( $member_dn, 0, $pos - 1 );

                my $groupdn;
                my @dn_parts = ldap_explode_dn($member_dn);
                for my $row ( 0 .. @dn_parts - 1 ) {
                    for my $col ( 0 .. @{$dn_parts[$row]}-1 ) {
                        if ( $col == 0 ) {
                            $groupdn = $dn_parts[$row][$col]{CN};
                        }
                        else {
                            $groupdn = $groupdn . ":" . $dn_parts[$row][$col]{OU};
                        }
                    }
                }
                $log->debug("Converted group: " . join( ".", reverse split( ":", $groupdn )));
                push( @uid, join( ":", reverse split( ":", $groupdn ) ) );
            }
            else {
                push( @uid, lc( ldap_explode_dn($member_dn)->[0]{CN} ) );
            }
        }
    }
    return @uid;
}


sub covertMemberDNListToMembersUidHash($ @) {
    my ( $target, @dn ) = @_;
    $log->debug(
        "Calling  CMU::Util::covertMemberDNListToMembersUidList($target, dn)");

    my %uid = ();

    foreach my $member_dn (@dn) {
        if ( $target eq "389" ) {
            if ( defined ldap_explode_dn($member_dn)->[0]{UID}
                 && length( ldap_explode_dn($member_dn)->[0]{UID} ) )
            {
                $uid{ lc( ldap_explode_dn($member_dn)->[0]{UID} ) } =
                    $member_dn;
            }
            else {
                $uid{ lc( ldap_explode_dn($member_dn)->[0]{CN} ) } = $member_dn;
            }
        }
        else {
            my $syncou = $CMU::CFG::_CFG{'AD'}{'syncou'};
            my $pos = index( lc($member_dn), lc($syncou) );
            if ( $pos >= 0 ) {
                my $partial_member_dn = substr( $member_dn, 0, $pos - 1 );

                my $groupdn;
                my @dn_parts = ldap_explode_dn($partial_member_dn);
                for my $row ( 0 .. @dn_parts - 1 ) {
                    for my $col ( 0 .. @{ $dn_parts[$row] } - 1 ) {
                        if ( $col == 0 ) {
                        	my @cn_parts = split(".", $dn_parts[$row][$col]{CN});
                            $groupdn = $cn_parts[0];
                        }
                        else {
                            $groupdn =
                                $groupdn . ":" . $dn_parts[$row][$col]{OU};
                        }
                    }
                }
                $uid{ join( ":", reverse split( ":", $groupdn ) ) } = $member_dn;
            }
            else {
                $uid{ lc( ldap_explode_dn($member_dn)->[0]{CN} ) } = $member_dn;
            }
        }
    }
    return %uid;
}

1;
