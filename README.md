# P2PFileSharing
Running
Setting up the file to transfer (if not already created)
The following command can be run from the root directory of this project to create the file this program will transport:

dd if=/dev/urandom of=<peer folder (i.e. "peer_1001/")>/<the file name> bs=<the size of the file>> count=1
Just be sure the parameters match up to what is specified in the config files

Start Remote Peers
Make sure you are on the VPN and can access the machines that the program accesses
you will encounter connectivity errors if you don't do this
Make sure the right branch has been checked out and updated on the remote servers
Change the variables in src/PeerMonitor.java to match your configuration
change userName to be your CISE username
change privateKeyPath to be the path to your private key that corresponds to the public key you put on the CISE machines
Run the compile command on the linux servers found in ./scripts/compileSrc
Make sure that the run command for starting PeerProcess found in PeerMonitor matches what is in the ./scripts/runPeerProcess script
i would have PeerProcess run the script directly, but the script files on checkout don't have the right permissions by default
this problem could be looked into, but for now I'm skipping this
Using the bash script, run ./scripts/runStartRemotePeers from the root directory of this project
Using IntelliJ, you can run the StartRemotePeers configuration
Peer Process Locally
Using the bash script, run ./scripts/runPeerProcess <peer id> in the root directory of this project
you don't need to be on the VPN to start this program
Using IntelliJ, run any of the PeerProcess configurations
