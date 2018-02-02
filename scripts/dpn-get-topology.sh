#!/bin/bash
# ------------------------------------------------------------------
#  Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#  This program and the accompanying materials are made available under the
#  terms of the Eclipse Public License v1.0 which accompanies this distribution,
#  and is available at http://www.eclipse.org/legal/epl-v10.html
# ------------------------------------------------------------------

if [ "$#" -ne 1 ]; then
    echo "Params incorrect, refer to the following template:"
    echo "./dpn-get-topology.sh dpn_type"
    exit 1
fi

dpn_type="$1"
case "$dpn_type" in
	sgwu|pgwu|spgw)
		;;
        *)
		echo "Values must be one of: 'sgwu', 'pgwu' or 'spgw'"
		exit 1
esac

echo ""
curl -i -s \
--header "Content-type: application/json" \
--request GET \
-u admin:admin \
http://localhost:8181/restconf/config/ietf-dmm-fpcagent:tenants/tenant/default/fpc-topology/dpn-types/$dpn_type
echo ""
