package com.homegrown;

import java.util.Properties;
import org.apache.log4j.Logger;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.ApplicationContext;
import com.homegrown.consumer.ConsumerCreator;
import com.homegrown.producer.ProducerCreator;
import com.homegrown.sampling.Sampler;


public final class Main {
	private static final Logger logger = Logger.getLogger (Main.class);
	private static ApplicationContext ctx;

	public static void main(String[] args) {
		ctx = new ClassPathXmlApplicationContext("producerContext.xml");
		Properties appProperties = (Properties) ctx.getBean("appProperties");
		String debugMsg = "App::";

		String brokers = appProperties.getProperty("brokers");
		String topic = appProperties.getProperty("topic");
		String role = appProperties.getProperty("role");
		String id = args.length>0?args[0]:appProperties.getProperty("id");
		if (role == null || role.isEmpty()) {
			logger.error(debugMsg+"No role is set in config file!!!");
		}else{
			logger.info(debugMsg+"Brokers: " + brokers);
			logger.info(debugMsg+"Topic: " + topic);
			logger.info(debugMsg+"Role: " + role);
			logger.info(debugMsg+"ID: " + id);

			if (role.equalsIgnoreCase("consumer")) {
				String group = appProperties.getProperty("group");
				String offsetReset = appProperties.getProperty("offsetReset");
				Integer pollingCount = Integer.parseInt(appProperties.getProperty("pollingCount"));

				logger.info(debugMsg+"Group: " + group);
				logger.info(debugMsg+"OffsetReset" + offsetReset);
				logger.info(debugMsg+"PollingCount" + pollingCount);

				runConsumer (brokers, topic, id, group, offsetReset, pollingCount);
			}else if (role.equalsIgnoreCase("producer")) {
				runProducer (brokers, topic, id);
			}else{
				logger.error(debugMsg+"Unknown role: "+role+"!!!");
			}
		}
	}

	private static void runConsumer (String brokers,String topic,String id,String group,String offsetReset,int pollingCount) {
		Consumer<String,byte[]> consumer = ConsumerCreator.createConsumer (brokers,topic,id,group,offsetReset,pollingCount);
		String debugMsg = "runConsumer::";
		Sampler sampler = new Sampler();
		double[] trainFFT = null;
		Properties appProperties = (Properties) ctx.getBean("appProperties");
		int transformationSize = Integer.parseInt(appProperties.getProperty("transformationSize"));
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

				if (trainFFT == null) {
					trainFFT = sampler.powerSpectrum(sampler.normalizeQuantiz(record.value()),transformationSize);
					System.out.println("Extracted train sample of size: " + record.value().length);
				}else{
					double[] testFFT = sampler.powerSpectrum(sampler.normalizeQuantiz(record.value()),transformationSize);
					double similarity = sampler.cosineSimilarity (testFFT,trainFFT);
					System.out.println("Extracted test sample of size: " + record.value().length);
					System.out.println("Similarity: " + (int)Math.round(100*similarity) + " %.");
				}
			}
			consumer.commitAsync();
		}
		//consumer.close();
	}

	private static void runProducer (String brokers,String topic,String id) {
		String debugMsg = "runProducer::";
		Producer<String,byte[]> producer = ProducerCreator.createProducer (brokers,id);
		Properties appProperties = (Properties) ctx.getBean("appProperties");

		/*float sampleRate = -1f;
		try {sampleRate = Aliasing.compute();}
		catch (Exception e) {
			e.printStackTrace();
			logger.error(debugMsg+e.getCause());
			logger.error(debugMsg+e.getMessage());
			sampleRate = Float.parseFloat(appProperties.getProperty("sampleRate"));
		}*/
		float sampleRate = Float.parseFloat(appProperties.getProperty("sampleRate"));
		int samplesNumber = Integer.parseInt(appProperties.getProperty("samplesNumber"));
		String recordingsDirectory = appProperties.getProperty("recordingsDirectory");
		Sampler sampler = new Sampler();

		while (true) {
			byte[] sample = sampler.record (sampleRate,samplesNumber,recordingsDirectory);
			double[] normalized = sampler.normalizeQuantiz(sample);
			StringBuffer print = new StringBuffer();
			for (int i=0; i<normalized.length; ++i) {
				print.append(",");
				print.append(normalized[i]);
			}
			if (sample.length > 0) {
				print.deleteCharAt(0);
			}
			logger.info(debugMsg+" normalized = ["+print.toString()+"];");
			final ProducerRecord<String,byte[]> record = new ProducerRecord<>(topic,id,sample);
			try{
				RecordMetadata metadata = producer.send(record).get();
				System.out.println("Record sent with key '" + id + "' to partition " + metadata.partition() + " with offset " + metadata.offset());
			}catch (ExecutionException e){
				System.out.println("Error in sending record");
				System.out.println(e);
			}catch (InterruptedException e){
				System.out.println("Error in sending record");
				System.out.println(e);
			}
		}
	}
}
