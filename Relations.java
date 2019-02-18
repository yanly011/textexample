package au.unisa.erl.textparse.textexample.controller;

public class Relations {
    private String subject;
    private String relation;
    private String arguement;
    private int sentenceNum;

    public Relations(String s, String r, String a, int n){
        this.subject = s;
        this.relation = r;
        this.arguement = a;
        this.sentenceNum = n;
    }

    public String getSubject() {
        return subject;
    }

    public String getRelation() {
        return relation;
    }

    public String getArguement() {
        return arguement;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public void setArguement(String arguement) {
        this.arguement = arguement;
    }

    public int getSentenceNum() {
        return sentenceNum;
    }

    public void setSentenceNum(int sentenceNum) {
        this.sentenceNum = sentenceNum;
    }

    @Override
    public String toString() {
        return "(\""+this.subject+"\" \""+this.relation+"\" \""+this.arguement+"\")";
    }
}