Executing the code:

mvn package

java -jar target/wordchain-1.0.SNAPSHOT.jar

Start tendermint
tendermint node

Testing:

Follow the commands in order.

curl -s 'localhost:46657/broadcast_tx_commit?tx="genesis"'
curl -s 'localhost:46657/broadcast_tx_commit?tx="strange"'
curl -s 'localhost:46657/broadcast_tx_commit?tx="elephant"'
curl -s 'localhost:46657/broadcast_tx_commit?tx="tendermint"'
curl -s 'localhost:46657/broadcast_tx_commit?tx="tendermint"'  - this should fail as Duplicate transaction
curl -s 'localhost:46657/broadcast_tx_commit?tx="hello"' - this should error out in check transaction itself as the transaction does not meet the specification.

