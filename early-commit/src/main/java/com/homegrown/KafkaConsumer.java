package com.homegrown;

import java.util.Map;
import java.util.TreeMap;
import java.util.Properties;
import java.util.Date;

import com.homegrown.util.DbService;
import com.homegrown.sampling.Sampler;
import com.homegrown.consumer.ConsumerCreator;
import org.springframework.web.context.ContextLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.log4j.Logger;


public class KafkaConsumer {
    private Properties appProperties;
    public Properties getAppProperties() {return appProperties;}
    public void setAppProperties(Properties appProperties) {this.appProperties = appProperties;}

    private DbService dbService;
    public DbService getDbService(){
        return dbService;
    }
    public void setDbService(DbService dbService){
        this.dbService = dbService;
    }


    private static final Logger logger = Logger.getLogger (Main.class);

    private Consumer<String,byte[]> consumer;

    private Map<String,double[]> trainFFTs = new TreeMap<>();
    private Map<String,double[]> testFFTs = new TreeMap<>();
    private Map<String,String> timestamps = new TreeMap<>();
    private Map<String,Integer> similarities = new TreeMap<>();

    public Map<String,double[]> getTrainFFTs () {return trainFFTs;}
    public Map<String,double[]> getTestFFTs () {return testFFTs;}
    public Map<String,String> getTimestamps () {return timestamps;}
    public Map<String,Integer> getSimilarities () {return similarities;}

    public synchronized void refresh () {
        if (consumer == null) {
            String brokers = appProperties.getProperty("brokers");
            String topic = appProperties.getProperty("topic");
            String role = appProperties.getProperty("role");
            String id = appProperties.getProperty("id");
            String group = appProperties.getProperty("group");
            String offsetReset = appProperties.getProperty("offsetReset");
            int pollingCount = Integer.parseInt(appProperties.getProperty("pollingCount"));
            int transformationSize = Integer.parseInt(appProperties.getProperty("transformationSize"));

            String debugMsg = "KafkaConsumer::";
            logger.info(debugMsg + "Brokers: " + brokers);
            logger.info(debugMsg + "Topic: " + topic);
            logger.info(debugMsg + "Role: " + role);
            logger.info(debugMsg + "ID: " + id);
            logger.info(debugMsg + "Group: " + group);
            logger.info(debugMsg + "OffsetReset" + offsetReset);
            logger.info(debugMsg + "PollingCount" + pollingCount);
            logger.info(debugMsg + "TransformationSize" + transformationSize);

            consumer = ConsumerCreator.createConsumer (brokers,topic,id,group,offsetReset,pollingCount);
        }
        String debugMsg = "refresh::";
        Sampler sampler = new Sampler();
        int pollingCount = Integer.parseInt(appProperties.getProperty("pollingCount"));
        int transformationSize = Integer.parseInt(appProperties.getProperty("transformationSize"));
        logger.info(debugMsg + "PollingCount" + pollingCount);
        logger.info(debugMsg + "TransformationSize" + transformationSize);
        int noMessageToFetch = 0;
        while (true) {
            final ConsumerRecords<String,byte[]> consumerRecords = consumer.poll (pollingCount);
            logger.info (debugMsg+"Brought "+consumerRecords.count()+" records...");
            if (consumerRecords.count() == 0) {
                noMessageToFetch++;
                if (noMessageToFetch > 100) break;
                else continue;
            }

            for (ConsumerRecord<String,byte[]> record : consumerRecords) {
                System.out.println("Record Key: " + record.key());
                System.out.println("Record partition: " + record.partition());
                System.out.println("Record offset: " + record.offset());

                double[] trainFFT = trainFFTs.get(record.key());
                if (trainFFT == null) {
                    trainFFT = sampler.powerSpectrum(sampler.normalizeQuantiz(record.value()),transformationSize);
                    System.out.println("Extracted train sample of size: " + record.value().length);

                    trainFFTs.put (record.key(),trainFFT);
                    timestamps.put (record.key(),java.util.Calendar.getInstance().getTime().toString());
                }else{
                    double[] testFFT = sampler.powerSpectrum(sampler.normalizeQuantiz(record.value()),transformationSize);
                    double similarity = sampler.cosineSimilarity (testFFT,trainFFT);
                    System.out.println("Extracted test sample of size: " + record.value().length);
                    System.out.println("Similarity: " + similarity + ".");

                    testFFTs.put(record.key(),testFFT);
                    similarities.put(record.key(),(int)Math.round(similarity*100));
                    timestamps.put (record.key(),new Date(record.timestamp()).toString());

                    dbService.insertSimilarity(record.key(),(int)Math.round(similarity*100));
                }
            }
            consumer.commitAsync();
        }
        //consumer.close();
    }

    @Override public void finalize () {if (consumer != null) consumer.close();}
}
