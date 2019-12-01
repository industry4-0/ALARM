package com.homegrown.services.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlRootElement
public class SimilaritiesResponseDto extends ResponseDto {

    private Map<String,double[]> trainFFTs;
    private Map<String,double[]> testFFTs;
    private Map<String,String> timestamps;
    private Map<String,Integer> similarities;

    public SimilaritiesResponseDto(){}
    public SimilaritiesResponseDto(String status, String message) {
        super(status,message);
    }

    public Map<String, double[]> getTrainFFTs() {
        return trainFFTs;
    }

    public void setTrainFFTs(Map<String, double[]> trainFFTs) {
        this.trainFFTs = trainFFTs;
    }

    public Map<String, double[]> getTestFFTs() {
        return testFFTs;
    }

    public void setTestFFTs(Map<String, double[]> testFFTs) {
        this.testFFTs = testFFTs;
    }

    public Map<String, String> getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(Map<String, String> timestamps) {
        this.timestamps = timestamps;
    }

    public Map<String, Integer> getSimilarities() {
        return similarities;
    }

    public void setSimilarities(Map<String, Integer> similarities) {
        this.similarities = similarities;
    }
}
