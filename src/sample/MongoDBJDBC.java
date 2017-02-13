package sample;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangzheng on 2017/1/24.
 */
public class MongoDBJDBC {
    private MongoCollection<Document> collection;
    private static Logger logger = Logger.getLogger(MongoDBJDBC.class);
    private static long no = 0;
    public MongoDBJDBC() {
        PropertyConfigurator.configure("log4j.properties");
        try {
            // 连接到 mongodb 服务
            MongoClient mongoClient = new MongoClient("localhost", 27017);
            logger.info("Get the service of mongodb successfully.");
            // 连接到数据库
            MongoDatabase mongoDatabase = mongoClient.getDatabase("test");
            logger.info("Connect to database successfully.");
            collection = mongoDatabase.getCollection("test");
            logger.info("Get the collection successfully.");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
    public void insert(List<Document> documents){
        //插入文档
        /**
         * 1. 创建文档 org.bson.Document 参数为key-value的格式
         * 2. 创建文档集合List<Document>
         * 3. 将文档集合插入数据库集合中 mongoCollection.insertMany(List<Document>) 插入单个文档可以用 mongoCollection.insertOne(Document)
         * */
        if (!documents.isEmpty()) {
            collection.insertMany(documents);
        }
    }
    public FindIterable<Document> selectAll(){
        FindIterable<Document> documents = collection.find();
        return documents;
    }
    public long getRecordsCount (){
        return collection.count();
    }

}
