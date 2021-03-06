./../get-topology.sh;
echo "create vdpn?";
read;
./cudVdpn.sh put;
echo "add dpns? wait 3 sec";
read;
./addDpnSimple.sh 1;
sleep 3;
./addDpnSimple.sh 2;
echo "bind client?";
read;
cd ..;
./bindclient.sh;
echo "create context?";
read;
./context_create.sh;
echo "update context?";
read;
./context_update.sh;
echo "delete context?";
read;
./context_delete.sh;
echo "remove dpns? wait 3 sec";
read;
cd dpn-mirrorScripts;
./removeDpnSimple.sh 2;
sleep 3;
./removeDpnSimple.sh 1;
echo "delete vdpn?";
read;
./cudVdpn.sh delete;
./../get-topology.sh;
echo "vdpnDemo is done :)"
