import { MongoClient } from 'mongodb';

const db_ip = process.argv[3];
const db_port = process.argv[4];

async function getDBClientAndGameState() {
    try {
        const mongoClient = new MongoClient("mongodb://" + db_ip + ":" + db_port);
        const database = mongoClient.db('against-all-db');
        const collection = database.collection('latest-map');
    
        const gameState = await collection.findOne({}, {sort:{_id:-1}});

        return {mongoClient, gameState}
    }
    catch (err) {
        console.log("❌[server]: An error has ocurred while connecting to the data base. The request could not be fulfilled.");
    }

    return {mongoClient: null, gameState: null}
}

export {
    getDBClientAndGameState
}