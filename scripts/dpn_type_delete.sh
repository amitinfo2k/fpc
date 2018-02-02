#!/usr/bin/python

import subprocess
import json
import sys

arg=sys.argv[1]
if (arg != "sgwu" and arg != "pgwu" and arg != "spgw"):
                print("Values must be one of: 'sgwu', 'pgwu' or 'spgw'")
                sys.exit(0)


topology_type=subprocess.check_output("curl -i \
--header \"Content-type: application/json\" \
--request GET \
-u admin:admin \
http://localhost:8181/restconf/config/ietf-dmm-fpcagent:tenants/tenant/default/fpc-topology/dpn-types/%s 2>/dev/null | grep \{.*\}" % (arg), shell=True)
#print topology

topology_json_t=json.loads(topology_type)

topology_json=topology_json_t['dpn-types'] 
	
if len(topology_json) != 0:
	for dpn_entry in topology_json:
	    dpn_types=dpn_entry['dpn-type-id']
	    #print json.dumps(dpn_entry, indent=4)
	    dpn_lt=dpn_entry['dpns']
	
	    #print "Deleting dpn-id named %s" % (dpn_id)
	
	    for dpn_entry_n in dpn_lt:
		dpn_id=dpn_entry_n['dpn-id']

		cmd="curl -i \
		--header \"Content-type: application/json\" \
		--request delete \
		-u admin:admin \
		--data '{\
		    \"dpn-type-id\": [\
				{\
				    \"dpn-type-id\": %s\
				}\
			]\
		    \"dpns\": [\
		        {\
		            \"dpn-id\": %s,\
		            \"dpn-name\": \"site1-anchor1\",\
		            \"dpn-groups\": [\
		            \"dpn-type\": %s,\
		                \"foo\"\
		            ],\
		            \"node-id\": \"node23\",\
		            \"network-id\": \"network22\"\
		        }\
		    ]\
		}' \
		http://localhost:8181/restconf/config/ietf-dmm-fpcagent:tenants/tenant/default/fpc-topology/dpn-types/%s" % (dpn_types, dpn_id, dpn_types,dpn_types)
		
		#print cmd
		
		print subprocess.check_output(cmd, shell=True)

topology=subprocess.check_output("curl -i \
--header \"Content-type: application/json\" \
--request GET \
-u admin:admin \
http://localhost:8181/restconf/config/ietf-dmm-fpcagent:tenants/tenant/default/fpc-topology 2>/dev/null | grep \{.*\}", shell=True)

topology_json=json.loads(topology)

print json.dumps(topology_json, indent=4)

