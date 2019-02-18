package au.unisa.erl.textparse.textexample.controller;

public class WordInformation {
    private String word;
    private String entity;
    private String postag;
    private String stem;
    private String clauseIdentifier;
    private String relationIdentifier;
    private int index;
    private int sentenceNum;

    public WordInformation(){

    }

    public WordInformation(String w, String e, String p, String st, String ci, String ri, int i, int s){
        this.word = w;
        this.entity = e;
        this.postag = p;
        this.index = i;
        this.sentenceNum = s;
        this.stem = st;
        this.clauseIdentifier = ci;
        this.relationIdentifier = ri;
    }

    public String getStem() {
        return stem;
    }

    public int getIndex() {
        return index;
    }

    public int getSentenceNum() {
        return sentenceNum;
    }

    public String getEntity() {
        return entity;
    }

    public String getPostag() {
        return postag;
    }

    public String getWord() {
        return word;
    }

    public String getClauseIdentifier() {
        return clauseIdentifier;
    }

    public String getRelationIdentifier() {
        return relationIdentifier;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setPostag(String postag) {
        this.postag = postag;
    }

    public void setSentenceNum(int sentenceNum) {
        this.sentenceNum = sentenceNum;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public void setStem(String stem){
        this.stem = stem;
    }

    public void setClauseIdentifier(String clauseIdentifier) {
        this.clauseIdentifier = clauseIdentifier;
    }

    public void setRelationIdentifier(String relationIdentifier) {
        this.relationIdentifier = relationIdentifier;
    }

    @Override
    public String toString() {
        return "This word is: " + this.word + "----entity type is: " + this.entity + "----pos tag is: " + this.postag + "----stem is: " + this.stem +"----located at the sentence of " +this.sentenceNum + "----the clause identifier is: " +this.clauseIdentifier + "----the relation identifier is: " +this.relationIdentifier + " -----and the position is: "+this.index;
    }
}
