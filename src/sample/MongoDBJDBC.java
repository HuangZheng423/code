package sample;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangzheng on 2017/1/24.
 */
public class MongoDBJDBC {
    private MongoCollection collection;

    private static Logger logger = Logger.getLogger(MongoDBJDBC.class);
    public MongoDBJDBC(String cName) {
        if (cName == null || cName.isEmpty()){
            cName = "default";
        }
        PropertyConfigurator.configure("log4j.properties");
        try {
            // 连接到 mongodb 服务
            MongoClient mongoClient = new MongoClient("localhost", 27017);
            logger.info("Get the service of mongodb successfully.");



            // 连接到数据库
            MongoDatabase mongoDatabase = mongoClient.getDatabase("test");
            logger.info("Connect to database successfully.");

            //连接到存放集合名的集合中
            MongoCollection cNameCollection = mongoDatabase.getCollection("cname");
            System.out.println(cNameCollection.count());
            if (cNameCollection == null){
                mongoDatabase.createCollection("cname");
                cNameCollection = mongoDatabase.getCollection("cname");
            }
            Document document = new Document();
            document.append("name",cName);
            //如果存放集合名的集合中没有当前集合则插入，并新建集合
            FindIterable<Document> iterable = cNameCollection.find(document);
            if (iterable.first() == null || iterable.first().isEmpty()){
                cNameCollection.insertOne(document);
                mongoDatabase.createCollection(cName);
            }
            //连接到当前集合
            this.collection = mongoDatabase.getCollection(cName);

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
        if (this.collection == null){
            System.out.println("c is null");
        }
        if (documents!=null && !documents.isEmpty()) {
            this.collection.insertMany(documents);
        }
    }
    public FindIterable<Document> selectAll(){
        FindIterable<Document> documents = this.collection.find();
        return documents;
    }
    public long getRecordsCount (){
        return this.collection.count();
    }

    public static void main(String[] args) {
        MongoDBJDBC mongoDBJDBC = new MongoDBJDBC("asd");
        Document document = new Document();
        document.append("a","b");
        FindIterable<Document> documents = mongoDBJDBC.selectAll();
    }

}
