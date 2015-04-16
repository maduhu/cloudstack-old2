#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

function usage() {
 echo ""
 echo "usage: ./package.sh [-p|--pack] [-r|--rc] [-h|--help] [ARGS]"
 echo ""
 echo "The commonly used Arguments are:"
 echo "oss|OSS         To package with only redistributable libraries (default)"
 echo "nonoss|NONOSS   To package with non-redistributable libraries"
 echo "rc              Build a release candidate number; place before -p"
 echo ""
 echo "Examples: ./package.sh -p|--pack oss|OSS"
 echo "          ./package.sh -p|--pack nonoss|NONOSS"
 echo "          ./package.sh -r 1 -p|--pack nonoss|NONOSS"
 echo "          ./package.sh (Default OSS)"
 exit 1
}

# RC is in case we want a release candidate
RC=""
function packaging() {
    CWD=`pwd`
    RPMDIR=$CWD/../../dist/rpmbuild
    PACK_PROJECT=cloudstack
    if [ -n "$1" ] ; then
      DEFOSSNOSS="-D_ossnoss nonoss"
    fi

    VERSION=`(cd ../../; mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version) | grep '^[0-9]\.'`
    RELEASE_REVISION="14.09.24"
    GQREL=$RELEASE_REVISION

    if [ "$RC" != "" ]; then
	GQREL="${GQREL}+rc${RC}"
    else
	# determine whether or not we are building a "snapshot" package, which
	# means not an officially tagged release.
	output=$(git describe --long --tags)
	version=$(echo $output | awk -F'-' '{print $1}')
	commits=$(echo $output | awk -F'-' '{print $2}')
	object=$(echo $output | awk -F'-' '{print $3}' | awk '{print substr($0, 2)}')

	if [ "$commits" -ne "0" ]; then
	    # this is a snapshot version and we need to mark it as 'nextver',
	    # which means this is for the next version to be released. rather
	    # than actually calculate the next version it is simpler (lazier) to
	    # just put nextver on the line.
	    GQREL="${GQREL}+nextver${commits}"
	fi
    fi

    if echo $VERSION | grep SNAPSHOT ; then
      REALVER=`echo $VERSION | cut -d '-' -f 1`
      DEFVER="-D_ver $REALVER"
      DEFPRE="-D_prerelease 1"
      DEFREL="-D_rel SNAPSHOT"
    else
      REALVER=`echo $VERSION`
      DEFVER="-D_ver $REALVER"
      DEFREL="-D_rel $GQREL"
    fi

    echo "Building ${DEFREL}"

    mkdir -p $RPMDIR/SPECS
    mkdir -p $RPMDIR/BUILD
    mkdir -p $RPMDIR/RPMS
    mkdir -p $RPMDIR/SRPMS
    mkdir -p $RPMDIR/SOURCES/$PACK_PROJECT-$VERSION


    (cd ../../; tar -c --exclude .git --exclude dist  .  | tar -C $RPMDIR/SOURCES/$PACK_PROJECT-$VERSION -x )
    (cd $RPMDIR/SOURCES/; tar -czf $PACK_PROJECT-$VERSION.tgz $PACK_PROJECT-$VERSION)

    cp cloud.spec $RPMDIR/SPECS

    (cd $RPMDIR; rpmbuild --define "_topdir $RPMDIR" "${DEFVER}" "${DEFREL}" ${DEFPRE+"${DEFPRE}"} ${DEFOSSNOSS+"$DEFOSSNOSS"} -bb SPECS/cloud.spec)

    exit
}


if [ $# -lt 1 ] ; then

	packaging

elif [ $# -gt 0 ] ; then
	SHORTOPTS="hp:r:"
	LONGOPTS="help,pack:,rc:"

	ARGS=$(getopt -s bash -u -a --options $SHORTOPTS  --longoptions $LONGOPTS --name $0 -- "$@" )
	eval set -- "$ARGS"

	while [ $# -gt 0 ] ; do
	case "$1" in
	-h | --help)
		usage
		exit 0
		;;
	-r | --rc)
		echo "Setting RC to $2"
		RC=$2
		shift
		;;
	-p | --pack)
		echo "Doing CloudStack Packaging ....."
		packageval=$2
		if [ "$packageval" == "oss" -o "$packageval" == "OSS" ] ; then
			packaging
		elif [ "$packageval" == "nonoss" -o "$packageval" == "NONOSS" ] ; then
			packaging nonoss
		else
			echo "Error: Incorrect value provided in package.sh script, Please see help ./package.sh --help|-h for more details."
			exit 1
		fi
		;;
	-)
		echo "Unrecognized option..."
		usage
		exit 1
		;;
	*)
		shift
		;;
	esac
	done

else
	echo "Incorrect choice.  Nothing to do." >&2
	echo "Please, execute ./package.sh --help for more help"
fi
