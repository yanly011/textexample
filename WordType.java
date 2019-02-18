package au.unisa.erl.textparse.textexample.controller;

public class WordType {
    private String word;
    private String type;
    private double probability;

    public WordType(){}

    public WordType(String word, String type, double prob){
        this.word = word;
        this.type = type;
        this.probability = prob;
    }

    public String getType() {
        return type;
    }

    public String getWord() {
        return word;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }
}
