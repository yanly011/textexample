package au.unisa.erl.textparse.textexample.controller;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import edu.knowitall.openie.Argument;
import edu.knowitall.openie.Instance;
import edu.knowitall.openie.OpenIE;
import edu.knowitall.tool.parse.ClearParser;
import edu.knowitall.tool.postag.ClearPostagger;
import edu.knowitall.tool.srl.ClearSrl;
import edu.knowitall.tool.tokenize.ClearTokenizer;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.Mention;
import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.ie.machinereading.structure.EntityMention;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations;
import edu.stanford.nlp.ie.machinereading.structure.RelationMention;
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.*;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.ChunkAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.ie.crf.CRFCliqueTree;
import edu.stanford.nlp.ie.machinereading.BasicRelationExtractor.*;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scalaz.Alpha;

import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import java.lang.Process;
import java.util.regex.Pattern;

@RestController
public class TextParserApi {

    private OpenIE openie;
    private CRFClassifier crfClassifier;
    private StanfordCoreNLP pipeline;
    private Set<String> pronounWords;
    private Set<String> unfirmedRelations;
    private Set<String> assumingWords;

    public TextParserApi() {
        super();
        this.openie = new OpenIE(new ClearParser(new ClearPostagger(new ClearTokenizer())), new ClearSrl(), false, false);
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        props.setProperty("ner.model", "english.all.3class.distsim.crf.ser.gz");
        props.setProperty("sutime.rules",  "defs.sutime.txt, english.holidays.sutime.txt, english.sutime.txt");
        props.setProperty("sutime.binders", "0");
        props.setProperty("ner.useSUTime", "0");
        this.pipeline = new StanfordCoreNLP(props);
        this.pronounWords  = new HashSet<String>(Arrays.asList("i", "he","she", "they", "it", "him", "her", "his", "its", "their", "them", "we", "us", "our", "you", "I", "these", "those", "what", "when", "where", "who", "whose", "this", "that", "here", "there"));
        this.unfirmedRelations = new HashSet<String>(Arrays.asList("will", "would"));
        this.assumingWords = new HashSet<String>(Arrays.asList("if", "whether", "as long as", "suggest", "wish", "request", "prefer"));
    }

    @RequestMapping(value = "parsetext" ,method = RequestMethod.GET)
    public String textMining(String text) throws IOException, InterruptedException, ClassNotFoundException {
//        System.out.println(text);
        text=text.trim();
        char lastChar = text.charAt(text.length()-1);
        if(Character.isLetterOrDigit(lastChar)){
            text = text + ".";
        }

        if(text.length()>3000){
            return "Sorry, the text is too long...";
        }

      /*  String origninalText = text;
        ArrayList<String> originalDividedSentences = dividSentence(origninalText, this.pipeline);*/

        text = coreferenceText(text, this.pipeline);
        System.out.println(text);
        ArrayList<String> dividedSentences = dividSentence(text, this.pipeline);
        /*for (String s:dividedSentences){
            System.out.println(s);
        }*/


        /*
        ArrayList<WordInformation> originalWordInformation = parseText(text, originalDividedSentences, this.pipeline);
        ArrayList<WordInformation> selectedSentences = selectSentence(replacedDividedSentences);
        ArrayList<WordInformation> clausIdentifiedSentences = identifyClause(selectedSentences);
        ArrayList<WordInformation> relationIdentifiedSentence = identifyRelation (clausIdentifiedSentences);
        */

        ArrayList<WordType> personWords = findSpecificTypeWords(text, "PERSON");  //find the word which type is "PERSON"
        ArrayList<WordType> organizationWords = findSpecificTypeWords(text, "ORGANIZATION");    //find the word which type is "ORGANIZATION"
        ArrayList<WordType> locationWords = findSpecificTypeWords(text, "LOCATION");    //find the word which type is "LOCATION"
        ArrayList<WordType> entityWords = personWords;
        entityWords.addAll(organizationWords);
        entityWords.addAll(locationWords);


        /*for(WordType w : entityWords){
            System.out.println("The word is: " + w.getWord() + " and the type is: " + w.getType() + " and the probability is: " + w.getProbability());
        }*/

        //ArrayList<String> personWords = findPersonNames(text.getInputText());
        /*if(entityWords.size() == 0){
            return "Sorry, no entity can be found...";
        }*/


       /* ArrayList<Relations> allRelations = new ArrayList<Relations>();
        int sentenceNum = 1;
        for(String sentence:dividedSentences) {
            System.out.println("Sentence " + sentenceNum + " is: " + sentence);
            ArrayList<Relations> relationsInSentence = findRelations(sentence, sentenceNum);
            allRelations.addAll(relationsInSentence);
            sentenceNum = sentenceNum + 1;
        }*/

        ArrayList<Relations> allRelations = findRelationsByOpenIE(dividedSentences, this.openie);

        System.out.println("Relation information before filtering: ---------------");
        int relationNum = 1;
        for (Relations r:allRelations){
            System.out.println(relationNum + ": " + r.toString());
//            System.out.println(" ");
            relationNum++;
        }
        System.out.println("--------------------------------------");
        ArrayList<Relations> filtedRelations = relationFilter(allRelations, this.pipeline, this.openie);
        System.out.println("Relation information after filtering: ---------------");
        relationNum = 1;
        for (Relations r:filtedRelations){
            System.out.println(relationNum + ": " + r.toString());
//            System.out.println(" ");
            relationNum++;
        }
        System.out.println("--------------------------------------");
        ArrayList<Entity> entities = generateEntity(filtedRelations, entityWords, dividedSentences);

        String json = new Gson().toJson(entities);
        System.out.println(json);
        return json;
    }


    @RequestMapping(value = "tagcontext" ,method = RequestMethod.GET)
    public String tagContext(String inputtext) throws IOException {

        char lastChar = inputtext.charAt(inputtext.length()-1);
        if(Character.isLetterOrDigit(lastChar)){
            inputtext=inputtext + ".";
        }
        String taggedText = identifyWordType(inputtext, this.pipeline);
        taggedText = tagDisplayInText(taggedText);
        taggedText = taggedText.replace("(tagbegin)PERSON(tagend)", "<span class=\"glyphicon glyphicon-user\"></span>");
        taggedText = taggedText.replace("(tagbegin)LOCATION(tagend)", "<span class=\"glyphicon glyphicon-home\"></span>");
        taggedText = taggedText.replace("(tagbegin)ORGANIZATION(tagend)", "<span class=\"glyphicon glyphicon-flag\"></span>");
        taggedText = taggedText.replace("(tagbegin)PHONENUMBER(tagend)", "<span class=\"glyphicon glyphicon-phone-alt\"></span>");
        taggedText = taggedText.replace("(tagbegin)VEHICLENUMBER(tagend)", "<i class=\"fa fa-automobile\" style=\"font-size:18px;\"></i>");
        taggedText = taggedText.replace("(tagbegin)DATE(tagend)", "<span class=\"glyphicon glyphicon-calendar\"></span>");
        taggedText = taggedText.replace("(tagbegin)DURATION(tagend)", "<i class=\"fa fa-clock-o\" style=\"font-size:18px\"></i>");
        taggedText = taggedText.replace("(tagbegin)NUMBER(tagend)", "<em><font size=\"2\" color=\"blue\">NUM</font></em>");
        taggedText = taggedText.replace("(tagbegin)", "<span class=\"bg-primary\">");
        taggedText = taggedText.replace("(tagend)", "</span>");
        taggedText = taggedText.replace("(wordbegin)", "<span class=\"bg-warning\">");
        taggedText = taggedText.replace("(wordend)", "</span>");

        return taggedText;
    }

    private ArrayList<WordType> findSpecificTypeWords(String originalText, String typeToFind) throws IOException, ClassNotFoundException {
//        String serializedClassifier =  this.getClass().getResource("classifiers/english.all.3class.distsim.crf.ser.gz").getPath();
        String serializedClassifier = "english.all.3class.distsim.crf.ser.gz";
        CRFClassifier classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
        List<List<CoreLabel>> out = classifier.classify(originalText);

        ArrayList<WordType> wordTypes = new ArrayList<WordType>();
        WordType currentWord = new WordType("", typeToFind, 0.0);

        for (List<CoreLabel> sentence : out) {
            CRFCliqueTree<String> cliqueTree = classifier.getCliqueTree(sentence);

            for (int i = 0; i < cliqueTree.length(); i++) {
                CoreLabel wi = sentence.get(i);
                double probOfEntity = 0.0;
                String word = wi.word();
                String ne = wi.get(CoreAnnotations.AnswerAnnotation.class);

                for (Iterator<String> iter = classifier.classIndex.iterator(); iter.hasNext(); ) {
                    String label = iter.next();
                    int index = classifier.classIndex.indexOf(label);
                    double prob = cliqueTree.prob(i, index);

                    if (prob > probOfEntity) {
                        probOfEntity = prob;
                    }
                }

//                System.out.println(word + "    " + ne + "    " + probOfEntity);
                if(ne.equals(typeToFind)){
                    if(currentWord.getWord() == ""){
                        currentWord.setWord(word);
                        currentWord.setProbability(probOfEntity);
//                        System.out.println("The Person name is start from : " + currentWord.getWord() + "    The type is: " + currentWord.getType() + "   Probability is: " + currentWord.getProbability());
                        }
                    else{
                        String tempWord = currentWord.getWord() + " " + word;
                        currentWord.setWord(tempWord);
                        if (currentWord.getProbability() < probOfEntity) {
                            currentWord.setProbability(probOfEntity);
                        }
//                        System.out.println("The Person name is changed to : " + currentWord.getWord() + "    The type is: " + currentWord.getType() + "   Probability is: " + currentWord.getProbability());
                    }
                }else{
                    if(currentWord.getWord() != ""){
                        WordType tempWordType = new WordType(currentWord.getWord(), currentWord.getType(), currentWord.getProbability());
                        Iterator<WordType> iterator = wordTypes.iterator();
                        boolean isHigher = false;
                        boolean isFound = false;
                        while(iterator.hasNext()){
                            WordType wt = iterator.next();
                            if(wt.getWord().equals(tempWordType.getWord()) && (wt.getProbability() < tempWordType.getProbability())){
//                                System.out.println("Found the duplicate name: " + wt.getWord() + "    " + wt.getProbability());
                                iterator.remove();
                                isHigher = true;
                            }else if(wt.getWord().equals(tempWordType.getWord())){
                                isFound = true;
                            }
                        }
                        if(isHigher || !isFound) {            //insert the current object to list when the probability is higher or no such a entity having same name
                            wordTypes.add(tempWordType);
                        }
//                        System.out.println("The Person name is end by : " + tempWordType.getWord() + "    The type is: " + tempWordType.getType() + "   Probability is: " + tempWordType.getProbability());
                        currentWord.setWord("");
                        currentWord.setProbability(0.0);
                    }
                }
                /*for(WordType w : wordTypes){
                    System.out.println(w.getWord() + "   " + w.getProbability() + "   " + w.getType());
                }*/
            }
            if(currentWord.getWord() != ""){
                Iterator<WordType> iterator = wordTypes.iterator();
                while(iterator.hasNext()){
                    WordType wt = iterator.next();
                    if(wt.getWord().equals(currentWord.getWord()) && (wt.getProbability() < currentWord.getProbability())){
                        iterator.remove();
                        WordType tempWordType = new WordType(currentWord.getWord(), currentWord.getType(), currentWord.getProbability());
                        wordTypes.add(tempWordType);
                    }
                }
                currentWord.setWord("");
                currentWord.setProbability(0.0);
            }
        }

        /*System.out.println(wordTypes.size());
        for(WordType w : wordTypes){
            System.out.println(w.getWord() + "   " + w.getProbability() + "   " + w.getType());
        }*/

        return wordTypes;
    }

    private ArrayList<String> findEntity(String originalText) throws IOException, ClassNotFoundException {
//        String serializedClassifier =  this.getClass().getResource("classifiers/english.all.3class.distsim.crf.ser.gz").getPath();
        String serializedClassifier = "english.all.3class.distsim.crf.ser.gz";
        CRFClassifier classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
        List<List<CoreLabel>> out = classifier.classify(originalText);


        ArrayList<String> names = new ArrayList<String>();
        String currentName = "";
        for (List<CoreLabel> sentence : out) {
            CRFCliqueTree<String> cliqueTree = classifier.getCliqueTree(sentence);

            for (int i = 0; i < cliqueTree.length(); i++) {
                CoreLabel wi = sentence.get(i);
                double probOfEntity = 0.0;
                String word = wi.word();
                String ne = wi.get(CoreAnnotations.AnswerAnnotation.class);
                for (Iterator<String> iter = classifier.classIndex.iterator(); iter.hasNext();) {
                    String label = iter.next();
                    int index = classifier.classIndex.indexOf(label);
                    double prob = cliqueTree.prob(i, index);
                    if(prob > probOfEntity){
                        probOfEntity = prob;
                    }
                }

                if (!("PERSON".equals(ne)) && (currentName!="")) {
                    currentName = currentName.trim();
                    names.add(currentName);
                    currentName = "";
                }else if("PERSON".equals(ne)){
                    currentName = currentName + word + " ";
                }
                if(!("O".equals(ne))) {
                    System.out.println(word + "/" + ne + "(" + probOfEntity + ")");
                }
            }

        }

        /*Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        props.setProperty("ner.model", "classifier/english.all.3class.distsim.crf.ser.gz");
        props.setProperty("sutime.rules",  "sutime/defs.sutime.txt, sutime/english.holidays.sutime.txt, sutime/english.sutime.txt");
        props.setProperty("sutime.binders", "0");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        props.setProperty("ner.useSUTime", "0");
        Annotation document = new Annotation(originalText);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);

        ArrayList<String> names = new ArrayList<String>();
        String currentName = "";
        for(CoreMap sentence: sentences) {

            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {

                String word = token.get(TextAnnotation.class);
                String ne = token.get(NamedEntityTagAnnotation.class);
                if (!("PERSON".equals(ne)) && (currentName!="")) {
                    currentName = currentName.trim();
                    names.add(currentName);
                    currentName = "";
                }else if("PERSON".equals(ne)){
                    currentName = currentName + word + " ";
                }
            }
        }*/

        LinkedHashSet<String> nameSet = new LinkedHashSet<String>(names);                 //Using hashset to unique the name list and remain the order
        ArrayList<String> listWithoutDuplicateNames = new ArrayList<String>(nameSet);

        return listWithoutDuplicateNames;
    }

/*    private ArrayList<String> mentionRefReplace(String originalText, ArrayList<String> dividedSentences){
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner, parse, mention, dcoref");
        props.setProperty("ner.model", "classifiers/english.all.3class.distsim.crf.ser.gz");
        props.setProperty("sutime.binders", "0");
        props.setProperty("ner.useSUTime", "0");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation document = new Annotation(originalText);
        pipeline.annotate(document);
        ArrayList<String> replacedDividedSentences = (ArrayList<String>) dividedSentences.clone();
        String mentionReplacedText = "";
        for (CorefChain cc : document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            int numOfMentions = 1;
            String mainMention = "";
            for(CorefChain.CorefMention cf:cc.getMentionsInTextualOrder()){
                if(numOfMentions == 1) {
                    mainMention = cf.mentionSpan;

                }
                if(numOfMentions > 1){
                    int sentenceNumber = cf.sentNum;
                    String s = replacedDividedSentences.get(sentenceNumber-1).replace(cf.mentionSpan, mainMention);
                    replacedDividedSentences.set(sentenceNumber-1, s);
                }

                numOfMentions++;
            }
        }
        *//*List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (Mention m : sentence.get(CorefCoreAnnotations.CorefMentionsAnnotation.class)) {
                System.out.println("\t" + m);
            }
        }*//*


        return replacedDividedSentences;
    }*/

    private String coreferenceText(String originalText, StanfordCoreNLP pipeline){

        Annotation document = new Annotation(originalText);
        pipeline.annotate(document);
        ArrayList<String> dividedSentences = dividSentence(originalText, pipeline);
//        ArrayList<String> replacedSentences = (ArrayList<String>) dividedSentences.clone();
        String mentionReplacedText = "";
        for (CorefChain cc : document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            System.out.println("The coreference chain is start:");
            int numOfMentions = 0;
//            double scoreOfNer = 0.0;
            String mainMention = "";
            String mainMentionType = "";
            for(CorefChain.CorefMention cf:cc.getMentionsInTextualOrder()){

                numOfMentions++;
                System.out.println(cf.mentionSpan + " and the type is: " + cf.mentionType);
//                System.out.println("Sentence number is: " + cf.sentNum);
//                System.out.println("Start Position: " + cf.position.get(0) + " and end position: " + cf.position.get(1));
                if(numOfMentions == 1) {
                    mainMention = cf.mentionSpan;
                    mainMentionType = cf.mentionType.toString();
//                    scoreOfNer = calScoreOfNerList(cf.mentionSpan, pipeline);
                }
                if(numOfMentions > 1){
//                    String pronoun = cf.mentionSpan.toLowerCase();
                    String mention = cf.mentionSpan;
                    String mentionType = cf.mentionType.toString();

                    if (mainMentionType.equals("PROPER")){
                        if (mentionType.equals("PROPER") && (mainMentionType.length() < mentionType.length())){
                            mainMention = mention;
                        }
                    }else if (mainMentionType.equals("NOMINAL")){
                        if (mentionType.equals("PROPER")){
                            mainMention = mention;
                            mainMentionType = mentionType;
                        }else if (mentionType.equals("NOMINAL") && (mainMentionType.length() < mentionType.length())){
                            mainMention = mention;
                        }
                    }

//                    System.out.println("mention is : " + mention);
//                    double newScore = calScoreOfNerList(mention, pipeline);
//                    System.out.println("The score of mention is: " + newScore);
//                    if((scoreOfNer < newScore)  && !mainMention.contains(mention)){
//                        scoreOfNer = newScore;
//                        mainMention = mention;
//                    }
//                    System.out.println("Current main mention is : " + mainMention);
//                    if (this.pronounWords.contains(pronoun)) {
//                        mentionReplacedText = originalText.replace(cf.mentionSpan, mainMention);
//                    }

                }


            }

            System.out.println("Current main mention is : " + mainMention + " and the type is: " + mainMentionType);
            int bias = 0;
            if (numOfMentions > 1) {
                int currentSentence = 0;
                for (CorefChain.CorefMention cf : cc.getMentionsInTextualOrder()) {
                    if (cf.sentNum != currentSentence){
                        currentSentence = cf.sentNum;
                        bias = 0;
                    }
                    String pronoun = cf.mentionSpan.toLowerCase();
                    String type = cf.mentionType.toString();
                    if (type.equals("PRONOMINAL")){
                        System.out.println(pronoun + " in the sentence " + cf.sentNum+ " and position is: " +  cf.startIndex + " and " + cf.endIndex + cf.mentionType.toString());

                        String replacedSentence = dividedSentences.get(cf.sentNum - 1);
                        System.out.println("The sentence need to replace mention is: " + replacedSentence);
                        Annotation mentionedReplaceSentence = new Annotation(replacedSentence);
                        pipeline.annotate(mentionedReplaceSentence);
                        List<CoreLabel> tokens = mentionedReplaceSentence.get(CoreAnnotations.TokensAnnotation.class);

                        int numOfToken = 1;
                        String sentenceBeforeMention = "";
                        String sentenceAfterMention = "";
                        String newReplacedSentence = "";
                        for (CoreLabel token:tokens){
                            if (numOfToken < (cf.startIndex)){
                                sentenceBeforeMention = sentenceBeforeMention + token.word() + " ";
                            }else if (numOfToken >= (cf.endIndex)){
                                sentenceAfterMention = sentenceAfterMention + " " + token.word();
                            }
                            numOfToken ++;
                        }

                        newReplacedSentence = sentenceBeforeMention + mainMention + sentenceAfterMention;
                        System.out.println("The sentence has been replaced mention is: " + newReplacedSentence);
                        dividedSentences.set(cf.sentNum - 1, newReplacedSentence);
                    }
                }
            }
            System.out.println("------------------------------------------------");
        }

        for (String s:dividedSentences){
            mentionReplacedText += s + " ";
        }
        return mentionReplacedText;
    }

    private double calScoreOfNerList (String mention, StanfordCoreNLP pipeline){
        double nerScore = 0.0;
        Annotation mentionText = new Annotation(mention);
        pipeline.annotate(mentionText);
        List<CoreLabel> tokens = mentionText.get(CoreAnnotations.TokensAnnotation.class);

        ArrayList<String> nerList =  new ArrayList<>();
        for (CoreLabel token : tokens) {
            String word = token.get(TextAnnotation.class);
            String ner = token.get(NamedEntityTagAnnotation.class);
//            System.out.println("Word is: " + word + " NER type is: " + ner);
            nerList.add(ner);
        }

        double scoreOfNer = 0.0;

        for(String ner:nerList){
            if ("PERSON".equals(ner)){
                scoreOfNer = scoreOfNer + 5.0;
            }else if("ORGANIZATION".equals(ner) || "LOCATION".equals(ner)){
                scoreOfNer = scoreOfNer + 3.0;
            }
//            System.out.println("Current score is: " + scoreOfNer);
        }

        nerScore = scoreOfNer / (nerList.size() * 5.0);
        return nerScore;
    }
    private ArrayList<WordInformation> parseText(String originalText, ArrayList<String> dividedSentences, StanfordCoreNLP pipeline){
        ArrayList<WordInformation> wordInformationList = new ArrayList<>();
        Annotation document = new Annotation(originalText);
        pipeline.annotate(document);
        ArrayList<String> replacedDividedSentences = (ArrayList<String>) dividedSentences.clone();
        for (CorefChain cc : document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            int numOfMentions = 1;
            String mainMention = "";
            for(CorefChain.CorefMention cf:cc.getMentionsInTextualOrder()){
                if(numOfMentions == 1) {
                    mainMention = cf.mentionSpan;

                }
                if(numOfMentions > 1){
                    String pronoun = cf.mentionSpan.toLowerCase();
                    if (pronoun.equals("he") || pronoun.equals("she") || pronoun.equals("they") || pronoun.equals("it") || pronoun.equals("these")) {
                        int sentenceNumber = cf.sentNum;
                        String s = replacedDividedSentences.get(sentenceNumber - 1).replace(cf.mentionSpan, mainMention);
                        replacedDividedSentences.set(sentenceNumber - 1, s);
                    }
                }

                numOfMentions++;
            }
        }
        String mentionReplacedText = "";
        for (String replacedSentence:replacedDividedSentences){
            mentionReplacedText+= " " + replacedSentence;
        }

        System.out.println("-----------------------------Mention replaced text:---------------------------------");
        System.out.println(mentionReplacedText);

        Annotation mentionedReplaceDocument = new Annotation(mentionReplacedText);
        pipeline.annotate(mentionedReplaceDocument);
        List<CoreMap> sentences = mentionedReplaceDocument.get(CoreAnnotations.SentencesAnnotation.class);
        int sentenceNumber = 1;
        for (CoreMap sentence : sentences) {
            int i = 1;
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String word = token.get(TextAnnotation.class);
                String postag = token.get(PartOfSpeechAnnotation.class);
                String ner = token.get(NamedEntityTagAnnotation.class);
                String stem = token.lemma();
                WordInformation wordInfo = new WordInformation(word, ner, postag, stem, "", "", i, sentenceNumber);
                wordInformationList.add(wordInfo);
                i++;
                System.out.println(wordInfo.toString());
            }
            sentenceNumber++;
        }
        return wordInformationList;
    }

    private ArrayList<WordInformation> selectSentence(ArrayList<WordInformation> wordInformationList){
        ArrayList<Integer> selectedSentenceIndexes = new ArrayList<>();
        ArrayList<WordInformation> wordOfSelectedSentences = new ArrayList<>();

        for (WordInformation wInfo:wordInformationList){
            if(wInfo.getPostag().contains("VB") && !(selectedSentenceIndexes.contains(wInfo.getSentenceNum()))){
                selectedSentenceIndexes.add(wInfo.getSentenceNum());
            }
        }

        for (Integer index:selectedSentenceIndexes){
            for (WordInformation wInfo:wordInformationList){
                if(wInfo.getSentenceNum() == index){
                    wordOfSelectedSentences.add(wInfo);
                }
            }
        }

        return wordOfSelectedSentences;
    }

    private WordInformation findWordByPosition(ArrayList<WordInformation> sentence, int position){
        WordInformation emptyWord = new WordInformation();
        for (WordInformation word:sentence){
            if (word.getIndex()==position) return word;
        }
        return emptyWord;
    }

    /*The tags of clause identifier:
    * BOSC: Begin of subject clause
    * IOSC: Inside of subject clause
    * BOAC: Begin of attributive clause
    * IOAC: Inside of attributive clause
    * BOOC: Begin of object clause
    * IOOC: Inside of object clause
    * */
    private ArrayList<WordInformation> identifyClause(ArrayList<WordInformation> wordOfSelectedSentences){
        ArrayList<WordInformation> wordOfIdentifiedClause = (ArrayList<WordInformation>) wordOfSelectedSentences.clone();
        ArrayList<WordInformation> currentSentence = new ArrayList<>();
        Integer currentSentenceNumber = 0;
        int index = 0;
        for (WordInformation wInfo: wordOfIdentifiedClause){
            index++;
            if (wInfo.getSentenceNum()!= currentSentenceNumber || index==wordOfIdentifiedClause.size()) {
                ArrayList<WordInformation> inSubjectClause = new ArrayList<>();
                ArrayList<WordInformation> inAttributiveClause = new ArrayList<>();
                ArrayList<WordInformation> inObjectClause = new ArrayList<>();
                for (WordInformation wi:currentSentence){
                    if (wi.getPostag().contains("W") || (wi.getPostag().equals("IN") && (wi.getStem().toLowerCase().equals("that")) || (wi.getWord().toLowerCase().equals("whether")))){
                        if (wi.getIndex()==1){
                            wi.setClauseIdentifier("BOSC");
                            inSubjectClause.add(wi);
                        }else if ((findWordByPosition(currentSentence,wi.getIndex()-1).getPostag().contains("VB"))||((findWordByPosition(currentSentence,wi.getIndex()-1).getPostag().equals("IN"))&&(findWordByPosition(currentSentence,wi.getIndex()-2).getPostag().contains("VB")))){
                            wi.setClauseIdentifier("BOOC");
                            inObjectClause.add(wi);
                        }else{
                            wi.setClauseIdentifier("BOAC");
                            inAttributiveClause.add(wi);
                        }
                    }else if (inSubjectClause.size()>0){
                        if (wi.getPostag().contains("VB")|| wi.getPostag().equals("MD")){
                            int verbNum = 0;
                            for (WordInformation w:inSubjectClause) {
                                if (w.getPostag().contains("VB") ) {
                                    WordInformation previousWord = findWordByPosition(currentSentence, w.getIndex() - 1);
//                                    WordInformation prePreviousWord = findWordByPosition(currentSentence, w.getIndex() - 2);
                                    if (!(previousWord.getPostag().contains("VB") ||  previousWord.getPostag().equals("CC"))) {
                                        verbNum++;
                                    }
                                }
                            }
                            WordInformation pWord = findWordByPosition(currentSentence, wi.getIndex() - 1);
                            if (verbNum == 1 && !(pWord.getPostag().contains("VB") || pWord.getPostag().equals("CC"))){
                                inSubjectClause.clear();
                            }else {
                                wi.setClauseIdentifier("IOSC");
                                inSubjectClause.add(wi);
                            }
                        }else {
                            wi.setClauseIdentifier("IOSC");
                            inSubjectClause.add(wi);
                        }
                    }else if (inAttributiveClause.size()>0){
                        if ((wi.getPostag().contains("VB")||(wi.getPostag().equals("MD"))) && findWordByPosition(currentSentence, wi.getIndex()-1).getWord().equals(",")){
                            inAttributiveClause.clear();
                        }else if(wi.getPostag().contains("VB")|| wi.getPostag().equals("MD")){
                            int verbNumber = 0;
                            for (WordInformation w:inAttributiveClause) {
                                if (w.getPostag().contains("VB") ) {
                                    WordInformation previousWord = findWordByPosition(currentSentence, w.getIndex() - 1);
                                    if (!(previousWord.getPostag().contains("VB") ||  previousWord.getPostag().equals("CC"))) {
                                        verbNumber++;
                                    }
                                }
                            }
                            WordInformation pWord = findWordByPosition(currentSentence, wi.getIndex() - 1);
                            if (verbNumber == 1 && !(pWord.getPostag().contains("VB") || pWord.getPostag().equals("CC"))){
                                inAttributiveClause.clear();
                            }else {
                                wi.setClauseIdentifier("IOAC");
                                inAttributiveClause.add(wi);
                            }
                        }else {
                            wi.setClauseIdentifier("IOAC");
                            inAttributiveClause.add(wi);
                        }
                    }else if (inObjectClause.size()>0){
                        wi.setClauseIdentifier("IOOC");
                        inObjectClause.add(wi);
                    }
                }
                currentSentenceNumber = wInfo.getSentenceNum();
                currentSentence.clear();
                currentSentence.add(wInfo);
            }else {
                currentSentence.add(wInfo);
            }

        }

        System.out.println("---------------------------------------Clause identified words--------------------------------------");
        for (WordInformation word:wordOfIdentifiedClause){
            System.out.println(word.toString());
        }

        return wordOfIdentifiedClause;
    }

    private ArrayList<ArrayList<WordInformation>> divideSentence(ArrayList<WordInformation> wordOfIdentifiedClause){
        ArrayList<ArrayList<WordInformation>> dividedSentenceList = new ArrayList<>();
        ArrayList<WordInformation> currentSentence = new ArrayList<>();
        int currentSentenceNumber = 1;
        for(WordInformation wi:wordOfIdentifiedClause){
            if (currentSentenceNumber<wi.getSentenceNum()){
                dividedSentenceList.add(currentSentence);
                currentSentence.clear();
                currentSentence.add(wi);
            }else {
                currentSentence.add(wi);
            }
        }
        dividedSentenceList.add(currentSentence);

        return dividedSentenceList;
    }

    private ArrayList<WordInformation> identifyRelation(ArrayList<WordInformation> wordOfSentences){
        ArrayList<WordInformation> wordOfIdentifiedRelation = (ArrayList<WordInformation>) wordOfSentences.clone();
        int index = 1;
        for (WordInformation wInfo: wordOfIdentifiedRelation){
            if (wInfo.getIndex()<index) continue;
            if (wInfo.getPostag().contains("VB") && wInfo.getRelationIdentifier().equals("")){
                System.out.println(wInfo.getWord());
                wInfo.setRelationIdentifier("RP");
                for (int i=wInfo.getIndex()-1; i>0; i--){
                    WordInformation preWord = findWordByPosition(wordOfIdentifiedRelation, i);
                    if (preWord.getPostag().contains("VB") || preWord.getPostag().equals("MD") || preWord.getPostag().equals("TO") || preWord.getPostag().contains("RB")){
                        System.out.println(preWord.getWord());
                        preWord.setRelationIdentifier("RP");
                    }else {
                        break;
                    }
                }
                for (int j=wInfo.getIndex()+1; j <= wordOfIdentifiedRelation.size(); j++){
                    WordInformation postWord = findWordByPosition(wordOfIdentifiedRelation, j);
                    if (postWord.getPostag().contains("VB") || postWord.getPostag().contains("RB") || postWord.getPostag().equals("TO") || postWord.getPostag().equals("IN")){
                        System.out.println(postWord.getWord());
                        postWord.setRelationIdentifier("RP");
                    }else {
                        index = j-1;
                        break;
                    }
                }
            }
            index++;
        }

        System.out.println("---------------------------------------Relation identified words--------------------------------------");
        for (WordInformation word:wordOfIdentifiedRelation){
            System.out.println(word.toString());
        }

        return wordOfIdentifiedRelation;
    }

    /*private ArrayList<WordInformation> subjectIdentification(ArrayList<WordInformation> wordOfIdentiedRelation){
        ArrayList<WordInformation> wordOfIdentifiedSubject

    }*/

    private ArrayList<Relations> findRelations2(String originalText, int sentenceNumber) {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
        props.setProperty("ner.model", "english.all.3class.distsim.crf.ser.gz");
        props.setProperty("sutime.rules",  "defs.sutime.txt, english.holidays.sutime.txt, english.sutime.txt");
        props.setProperty("sutime.binders", "0");
        props.setProperty("ner.useSUTime", "0");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        Annotation document = new Annotation(originalText);
        pipeline.annotate(document);
        ArrayList<Relations> relations = new ArrayList<Relations>();
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            ArrayList<String> candidateEntities = new ArrayList<String>();
            ArrayList<String> candidateRelations = new ArrayList<String>();

            String e = "";
            String t = "";

            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String word = token.get(TextAnnotation.class);
                String postag = token.get(PartOfSpeechAnnotation.class);
                String ner = token.get(NamedEntityTagAnnotation.class);
                String chunk = token.get(ChunkAnnotation.class);
                String lemma = token.lemma();
                System.out.println("The word: "+word+"'s postage is " + postag);
                System.out.println("The word: "+word+"'s type is " + ner);
                System.out.println("The word: "+word+"'s chunk tag is " + chunk);
                System.out.println("The word: "+word+"'s stem is " + lemma);
                if(postag.contains("VB")){
                    candidateRelations.add(word);
                }
                if(ner!=null) {
                    if (!"O".equals(ner) && !ner.equals(t) && e != "") {
                        candidateEntities.add(e);
                        e = word;
                        t = ner;
                    } else if (!"O".equals(ner) && !ner.equals(t) && e == "") {
                        e = word;
                        t = ner;
                    } else if (!"O".equals(ner) && ner.equals(t)) {
                        e = e + " " + word;
                    } else if ("O".equals(ner) && e != "") {
                        candidateEntities.add(e);
                        e = "";
                        t = "";
                    }
                }
            }

            for (String rel:candidateRelations){
                for (String sub:candidateEntities){
                    for (String obj:candidateEntities){
                        if(!sub.equals(obj)){
                            Relations r = new Relations(sub, rel, obj, sentenceNumber);
                            relations.add(r);
                        }
                    }
                }
            }
        }

        for (Relations r:relations){
            System.out.println(r.toString());
        }
        return relations;
    }

    private ArrayList<Relations> relationFilter(ArrayList<Relations> originalRelations, StanfordCoreNLP pipeline, OpenIE openIE){
        ArrayList<Relations> completedRelations = uncompletedRelationFiler(originalRelations);
        ArrayList<Relations> uncontainedRelations = containedRelationFilter(completedRelations);
        ArrayList<Relations> nonAdjRelations = beAdjRelationFilter(uncontainedRelations, pipeline);
        ArrayList<Relations> relations = assumedRelationFilter(nonAdjRelations);
        ArrayList<Relations> r = assumedRelationsFromArgumensFilter(relations, pipeline, openIE);
        return r;
    }

    //delete the relation that its argument is contained by other relation
    private ArrayList<Relations> containedRelationFilter(ArrayList<Relations> originalRelations){
        ArrayList<Relations> relations = new ArrayList<>();
//        System.out.println("Start to compare relations...");
        for (Relations r:originalRelations){
//            System.out.println(r.toString());
            boolean isContained = false;
            for (Relations comparedRelation:originalRelations){
                if (r.getSubject().equals(comparedRelation.getSubject()) && r.getRelation().equals(comparedRelation.getRelation())  && comparedRelation.getArguement().equals(r.getArguement())){
                    continue;
                }else if (r.getSubject().equals(comparedRelation.getSubject()) && r.getRelation().equals(comparedRelation.getRelation())  && (comparedRelation.getArguement().length() > r.getArguement().length())){
                    isContained = true;
                    break;
                }else if(r.getSubject().equals(comparedRelation.getSubject())){
                    String s1 = r.getRelation()+" "+r.getArguement();
                    String s2 = comparedRelation.getRelation()+" "+comparedRelation.getArguement();
                    if (s2.contains(s1)){
                        isContained = true;
                        break;
                    }
                }
            }
            if (!isContained){
                relations.add(r);
            }
        }
        return relations;
    }

    //delete the relation like be + adjective
    private ArrayList<Relations> beAdjRelationFilter(ArrayList<Relations> originalRelations, StanfordCoreNLP pipeline){
        ArrayList<Relations> relations = new ArrayList<>();
        for (Relations r:originalRelations){
            String relation = r.getRelation().toLowerCase();
//            if (relation.contains("is") || relation.contains("are") || relation.contains("am") || relation.contains("was") || relation.contains("were") || relation.contains("will be")){
            String argument = r.getArguement();

            ArrayList<String> posTagList = new ArrayList<>();
            Annotation document = new Annotation(argument);
            pipeline.annotate(document);
            List<CoreLabel> tokens = document.get(CoreAnnotations.TokensAnnotation.class);

            for (CoreLabel token : tokens) {
//                String word = token.get(TextAnnotation.class);
                String pos = token.get(PartOfSpeechAnnotation.class);
                posTagList.add(pos);
//                System.out.println(word + "    " + pos);
            }


            boolean isAdjNoun = true;
//            int numOfTokenReviewed = 1;
            for (String pos:posTagList){
//                if (numOfTokenReviewed > 1) break;
                if (pos.equals("WRB")) break;
                if (pos.contains("NN")){
                    isAdjNoun = false;
                    break;
                }
//              else if (pos.equals("IN") || (pos.equals("DT"))){
//                    numOfTokenReviewed --;
//                }
//                numOfTokenReviewed ++;
            }

            if (!isAdjNoun){
                relations.add(r);
            }
            /*}else {
                relations.add(r);
            }*/
        }
        return relations;
    }

    private ArrayList<Relations> uncompletedRelationFiler(ArrayList<Relations> originalRelations){
        ArrayList<Relations> relations = new ArrayList<>();

        for (Relations r:originalRelations){
            if (r.getSubject().equals("") || r.getRelation().equals("") || r.getArguement().equals("") || this.pronounWords.contains(r.getSubject().toLowerCase())){
            }else {
                relations.add(r);
            }
        }

        return relations;
    }

    private ArrayList<Relations> assumedRelationFilter(ArrayList<Relations> originalRelations){
        ArrayList<Relations> relations = new ArrayList<>();

        for (Relations r:originalRelations){
            String relation = r.getRelation().toLowerCase();
            if(relation.contains("will") || relation.contains("would") || relation.contains("may") || relation.contains("should")){
                continue;
            }else{
                relations.add(r);
            }
        }
        return relations;
    }


    private ArrayList<Relations> assumedRelationsExtraction(String sentence, StanfordCoreNLP pipeline, OpenIE openIE, int sentenceNumber){

        Annotation document = new Annotation(sentence);
        pipeline.annotate(document);
        List<CoreLabel> words = document.get(TokensAnnotation.class);
        int position = 1;
        boolean inClause = false;
        boolean hasNounInClause = false;
        boolean hasVerbInClause = false;

        ArrayList<String> clauses = new ArrayList<>();
        String tempClause = "";
        for (CoreLabel token : words) {
            String word = token.get(TextAnnotation.class);
            String postag = token.get(PartOfSpeechAnnotation.class);
            String stem = token.lemma();

            if (word.toLowerCase().equals("assuming") && position ==1){
                inClause = true;
            }

            else if(this.assumingWords.contains(stem.toLowerCase()) && !inClause){
                inClause = true;
            }

            else if (postag.contains("VB") && inClause){
                hasVerbInClause = true;
                tempClause = tempClause  + word + " ";
            }

            else if (postag.contains("NN") && inClause){
                hasNounInClause = true;
                tempClause = tempClause  + word + " ";
            }

            else if ((word.contains(",") || word.contains(".") || position == words.size())  && (hasNounInClause && hasVerbInClause) && inClause){
                tempClause = tempClause  + word + " ";
                System.out.println("One assumed clause is found: " + tempClause);
                clauses.add(tempClause);
                tempClause = "";
                inClause = false;
                hasNounInClause = false;
                hasVerbInClause = false;
            }

            else if (inClause){
                tempClause = tempClause  + word + " ";
            }

            position ++;
        }

        ArrayList<Relations> assumedRelations = new ArrayList<>();

        for(String s:clauses) {
            System.out.println("-------------------------------------------------------------------------------");
            System.out.println(s);
            Seq<Instance> extractions = openIE.extract(s);

            List<Instance> list_extractions = JavaConversions.seqAsJavaList(extractions);

            for (Instance instance : list_extractions) {
                StringBuilder sb = new StringBuilder();

                sb.append(instance.confidence())
                        .append(" \"")
                        .append(instance.extr().arg1().text())
                        .append("\" \"")
                        .append(instance.extr().rel().text())
                        .append("\" \"");

                List<Argument> list_arg2s = JavaConversions.seqAsJavaList(instance.extr().arg2s());
                String arguments = "";
                for (Argument argument : list_arg2s) {
                    sb.append(argument.text()).append("; ");
                    arguments += argument.text() + "; ";
                }
                sb.append("\"");
                Relations r = new Relations(instance.extr().arg1().text(), instance.extr().rel().text(), arguments, sentenceNumber);
                assumedRelations.add(r);
            }
        }
        System.out.println("Assuming relations are: ");
        for (Relations r:assumedRelations){
            System.out.println(r.toString());
        }

        return assumedRelations;
    }

    private ArrayList<Relations> assumedRelationsFromArgumensFilter(ArrayList<Relations> inputRelations, StanfordCoreNLP pipeline, OpenIE openIE){
        ArrayList<Relations> relationsWithoutAssuming = new ArrayList<>();
        ArrayList<Relations> relationsOfAssuming = new ArrayList<>();
        for (Relations r:inputRelations) {
            String relation = r.getRelation();
            Annotation document = new Annotation(relation);
            pipeline.annotate(document);
            List<CoreLabel> words = document.get(TokensAnnotation.class);
            boolean contained = false;
            for (CoreLabel token : words) {
                String word = token.get(TextAnnotation.class);
                String postag = token.get(PartOfSpeechAnnotation.class);
                String stem = token.lemma();
                if (this.assumingWords.contains(stem.toLowerCase())) {
                    contained = true;
                    break;
                }
                if (contained) {
                    String arg = r.getArguement();
                    int sentenceNumber = r.getSentenceNum();
                    Seq<Instance> extractions = openIE.extract(arg);

                    List<Instance> list_extractions = JavaConversions.seqAsJavaList(extractions);

                    for (Instance instance : list_extractions) {
                        StringBuilder sb = new StringBuilder();

                        sb.append(instance.confidence())
                                .append(" \"")
                                .append(instance.extr().arg1().text())
                                .append("\" \"")
                                .append(instance.extr().rel().text())
                                .append("\" \"");

                        List<Argument> list_arg2s = JavaConversions.seqAsJavaList(instance.extr().arg2s());
                        String arguments = "";
                        for (Argument argument : list_arg2s) {
                            sb.append(argument.text()).append("; ");
                            arguments += argument.text() + "; ";
                        }
                        sb.append("\"");
                        Relations re = new Relations(instance.extr().arg1().text(), instance.extr().rel().text(), arguments, sentenceNumber);
                        relationsOfAssuming.add(re);
                    }
                }
            }
        }
        for (Relations r:inputRelations){
            boolean isEqual = false;
            for (Relations comparedRelation:relationsOfAssuming){
                if (getSimilarity(r, comparedRelation, pipeline) > 0.9){
                    isEqual = true;
                    break;
                }
            }
            if (!isEqual){
                relationsWithoutAssuming.add(r);
            }
        }

        return relationsWithoutAssuming;
    }

    //using Open IE 4.x to extract relations from text
    private ArrayList<Relations> findRelationsByOpenIE(ArrayList<String> dividedSentences, OpenIE openIE){
//        OpenIE openIE = new OpenIE(new ClearParser(new ClearPostagger(new ClearTokenizer())), new ClearSrl(), false, false);
        ArrayList<Relations> identifiedRelations = new ArrayList<>();
        int sentenceNumber = 1;
        for(String s:dividedSentences){
            ArrayList<Relations> relationsInCurrentSentence = new ArrayList<>();
            System.out.println("-------------------------------------------------------------------------------");
            System.out.println(s);
            Seq<Instance> extractions = openIE.extract(s);

            List<Instance> list_extractions = JavaConversions.seqAsJavaList(extractions);

            for(Instance instance : list_extractions) {
                StringBuilder sb = new StringBuilder();

                sb.append(instance.confidence())
                        .append(" \"")
                        .append(instance.extr().arg1().text())
                        .append("\" \"")
                        .append(instance.extr().rel().text())
                        .append("\" \"");

                List<Argument> list_arg2s = JavaConversions.seqAsJavaList(instance.extr().arg2s());
                String arguments = "";
                for(Argument argument : list_arg2s) {
                    sb.append(argument.text()).append("; ");
                    arguments += argument.text() + "; ";
                }
                sb.append("\"");
                Relations r = new Relations(instance.extr().arg1().text(), instance.extr().rel().text(), arguments, sentenceNumber);
                relationsInCurrentSentence.add(r);
//                System.out.println(sb.toString());
            }

            ArrayList<Relations> assumedRelationsInSentence = assumedRelationsExtraction(s, this.pipeline, openIE, sentenceNumber);
            ArrayList<Relations> relationsWithoutAssumption = new ArrayList<>();

            for (Relations r:relationsInCurrentSentence){
                boolean assumed = false;
                for (Relations ar:assumedRelationsInSentence){
                    if (r.getSubject().equals(ar.getSubject()) && r.getRelation().equals(ar.getRelation()) && r.getArguement().equals(ar.getArguement())){
                        assumed = true;
                        break;
                    }
                }
                if (!assumed){
                    relationsWithoutAssumption.add(r);
                }
            }
            identifiedRelations.addAll(relationsWithoutAssumption);

            sentenceNumber++;
        }


        return identifiedRelations;
    }

    //Using clausieapp to extract relations from text and record the number of sentence
    private ArrayList<Relations> findRelations(String originalText, int sentenceNumber) throws IOException, InterruptedException {
        String mediaFilePath = "C:\\Users\\yangl\\Documents\\text\\mediafile.txt";
        File write = new File(mediaFilePath);
        write.createNewFile();
        BufferedWriter out = new BufferedWriter(new FileWriter(write));
        out.write(originalText);
        out.close();
        String clausCommand = "java -jar C:\\Users\\yangl\\Documents\\myprojects\\clausieapp\\out\\artifacts\\clausieapp_jar\\clausieapp.jar " + mediaFilePath;
        Process clausieProcess = Runtime.getRuntime().exec(clausCommand);
        //clausieProcess.waitFor();
        String json = "";
        String j = "";
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clausieProcess.getInputStream()));
        while((j=bufferedReader.readLine()) != null){
            json = json + j;
//            System.out.println(j);
        }



//        Runtime.getRuntime().exec(clausCommand).waitFor();
//        String mediaResultPath = mediaFilePath.replace(".txt", "_result.json");
//        File read = new File(mediaResultPath);
//        String json = FileUtils.readFileToString(read);


        Gson gson = new Gson();
        ArrayList<Relations> relations = gson.fromJson(json, new TypeToken<List<Relations>>() {}.getType());
        for (Relations r:relations
             ) {
            r.setSentenceNum(sentenceNumber);
        }

        /*JsonReader jsonReader = new JsonReader(new StringReader(json));
        jsonReader.setLenient(true);
        ArrayList<Relations> relations = new ArrayList<Relations>();

        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            Relations r = new Relations();
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String elementOfRelation = jsonReader.nextName();
                if(elementOfRelation.equals("subject")) {
                    r.setSubject(jsonReader.nextString());
                } else if(elementOfRelation.equals("relation")) {
                    r.setRelation(jsonReader.nextString());
                } else if(elementOfRelation.equals("arguement")) {
                    r.setArguement(jsonReader.nextString());
                }
            }

            r.setSentenceNum(sentenceNumber);
            //System.out.println("Current sentence number is: " + r.getSentenceNum());
            relations.add(r);
            jsonReader.endObject();
        }
        jsonReader.endArray();*/

        return relations;
    }


    private ArrayList<Entity> generateEntity(ArrayList<Relations> relations, ArrayList<WordType> names, ArrayList<String> sentences) throws IOException {
        ArrayList<Entity> personEntities = new ArrayList<Entity>();

        for(WordType name: names){
            Entity personEntity =  new Entity();
            long time = System.currentTimeMillis();
            String t = String.valueOf(time/1000);
            personEntity.setId(t);
            personEntity.setName(name.getWord());
            personEntity.setEntityType(name.getType());
            personEntity.setTypeProbability(String.format("%.3f",name.getProbability()));
            ArrayList<Relations> entityRelations = findEntityRelations(name.getWord(), relations);     //find the relation for this entity's name
            /*System.out.println("Find relations of the entity of "+name.getWord());
            for(Relations r:entityRelations){
                System.out.println(r.toString());
            }*/
            if(entityRelations == null){
                continue;
            }
//            System.out.println("Current sentence number is: " + personRelations.getSentenceNum());
            ArrayList<Relations> tempRelationList = new ArrayList<Relations>();
            ArrayList<String> tempOriginalContextList = new ArrayList<String>();
            for (Relations tempR:entityRelations) {
                int sentenceNumber = tempR.getSentenceNum();
                String tempSubject = tempR.getSubject();
                int index = sentenceNumber-1;
                tempR.setSubject(tempSubject);
                tempR.setRelation(tempR.getRelation());
                tempR.setSentenceNum(sentenceNumber);
                if (tempR.getArguement() != null) {
                    //Another option that the subject and argument of relation are tagged.
                /*String tempSubject = personRelations.getSubject();
                String tempArguement = personRelations.getArguement();
                tempSubject = identifyWordTypeWithProb(tempSubject.trim());
                String taggedSubject = tagDisplayInText(tempSubject);
                tempArguement = identifyWordTypeWithProb(tempArguement.trim());
                String taggedArguement = tagDisplayInText(tempArguement);
                tempR.setSubject(taggedSubject);//transfer the subject of the longest relation to json string of wordtype list
                tempR.setRelation(personRelations.getRelation());
                tempR.setArguement(taggedArguement);//transfer the longest arguement to json string of wordtype list
                tempR.setSentenceNum(sentenceNumber);*/

                    String tempArguement = tempR.getArguement();
                    tempR.setArguement(tempArguement);
                }

                tempRelationList.add(tempR);
//            System.out.println("The sentence which will be tagged is: " + sentences.get(index));
                if (!tempOriginalContextList.contains(sentences.get(index))) {
                    tempOriginalContextList.add(sentences.get(index));
//                    String tempContext = identifyWordType(sentences.get(index));
//                    String taggedContext = tagDisplayInText(tempContext);
//                    tempContextList.add(taggedContext);
                }
            }

            personEntity.setLongestRelation(tempRelationList);
            personEntity.setOriginalContext(tempOriginalContextList);
            personEntities.add(personEntity);
        }

        return personEntities;
    }

    private ArrayList<Relations> findEntityRelations(String name, ArrayList<Relations> relations){ //find the relation related to the entity's name, which has the LONGEST arguement
        ArrayList<Relations> extractRelations = new ArrayList<Relations>();
        for(Relations r: relations){
            if(r.getSubject().contains(name)||r.getArguement().contains(name)){
                extractRelations.add(r);
            }
        }
        return extractRelations;

        /*if(extractRelations.size() == 0){
            return null;
        }

        Relations longestRelation = new Relations();
        String longestString = "";
        for(Relations cr: extractRelations) {
            if (cr.getArguement() != null) {
                String temp = cr.getSubject() + cr.getRelation() + cr.getArguement();
                if (temp.length() > longestString.length()) {
                    longestRelation.setSentenceNum(cr.getSentenceNum());
                    longestRelation.setSubject(cr.getSubject());
                    longestRelation.setRelation(cr.getRelation());
                    longestRelation.setArguement(cr.getArguement());
                    longestString = temp;
                }
            }else {
                String temp = cr.getSubject() + cr.getRelation();
                if (temp.length() > longestString.length()) {
                    longestRelation.setSentenceNum(cr.getSentenceNum());
                    longestRelation.setSubject(cr.getSubject());
                    longestRelation.setRelation(cr.getRelation());
                    longestString = temp;
                }
            }
        }

//        System.out.println("the sentence number of longest relation of " + longestRelation.getSubject() + " is: " + longestRelation.getSentenceNum());
        return longestRelation;*/
    }

    public String identifyWordType(String inputText, StanfordCoreNLP pipeline){
        /*Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        props.setProperty("ner.model", "classifiers/english.all.3class.distsim.crf.ser.gz");
        props.setProperty("sutime.rules",  "sutime/defs.sutime.txt, sutime/english.holidays.sutime.txt, sutime/english.sutime.txt");
        props.setProperty("sutime.binders", "0");
        props.setProperty("ner.useSUTime", "0");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);*/

        Annotation document = new Annotation(inputText);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        ArrayList<WordType> wordTypes = new ArrayList<WordType>();
        String phoneNumberPattern = "^\\({0,1}((0|\\+61)(2|4|3|7|8)){0,1}\\){0,1}(\\ |-){0,1}[0-9]{2}(\\ |-){0,1}[0-9]{2}(\\ |-){0,1}[0-9]{1}(\\ |-){0,1}[0-9]{3}$";
        String registNumberPattern = "\\b[a-zA-Z]{3}[-.]?\\d{2}[a-zA-Z]\\b|\\b[a-zA-Z]{1,2}[-.]?\\d{2}[-.]?[a-zA-Z]{2}\\b|\\b[a-zA-Z]\\d{3}[-.]?[a-zA-Z]{3}\\b|\\b[a-zA-Z]{2}[-.]?\\d{3}[-.]?[a-zA-Z] \\b|\\b\\d[a-zA-Z]{2}[-.]?\\d[a-zA-Z]{2} \\b|\\b\\d[a-zA-Z]{3}[-.]?\\d{3} \\b";

        for(CoreMap sentence: sentences) {
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String word = token.get(TextAnnotation.class);
                if(Pattern.matches(phoneNumberPattern, word)){
                    WordType et = new WordType(word, "PHONENUMBER", 0.0);
                    wordTypes.add(et);
                    continue;
                }
                if(Pattern.matches(registNumberPattern, word)){
                    WordType et = new WordType(word, "VEHICLENUMBER", 0.0);
                    wordTypes.add(et);
                    continue;
                }
                String ne = token.get(NamedEntityTagAnnotation.class);
                WordType et = new WordType(word, ne, 0.0);
                wordTypes.add(et);
            }
        }

        String json = new Gson().toJson(wordTypes);
        return json;
    }

    private  ArrayList<WordType> identifyWordTypeWithProb(String inputText){
        String serializedClassifier =  "english.all.3class.distsim.crf.ser.gz";
        CRFClassifier classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
        List<List<CoreLabel>> out = classifier.classify(inputText);
        ArrayList<WordType> wordTypes = new ArrayList<WordType>();
        for (List<CoreLabel> sentence : out) {
            CRFCliqueTree<String> cliqueTree = classifier.getCliqueTree(sentence);
            for (int i = 0; i < cliqueTree.length(); i++) {
                CoreLabel wi = sentence.get(i);
                double probOfEntity = 0.0;
                String word = wi.word();
                String ne = wi.get(CoreAnnotations.AnswerAnnotation.class);
                for (Iterator<String> iter = classifier.classIndex.iterator(); iter.hasNext();) {
                    String label = iter.next();
                    int index = classifier.classIndex.indexOf(label);
                    double prob = cliqueTree.prob(i, index);
                    if(prob > probOfEntity){
                        probOfEntity = prob;
                    }
                }
                WordType et = new WordType(word, ne, probOfEntity);
                if(!ne.equals("O")) {
                    wordTypes.add(et);
                }
            }

        }

//        String json = new Gson().toJson(wordTypes);
        return wordTypes;
    }

    public String tagDisplayInText(String wordTypes) throws IOException {        //display the tagged sentence
        String taggedText = "";

        Gson gson = new Gson();
        List<WordType> wordTypeList = gson.fromJson(wordTypes, new TypeToken<List<WordType>>() {}.getType());
        /*//Parsing the json string to Arraylist<WordType>
        JsonReader jsonReader = new JsonReader(new StringReader(wordTypes));
        jsonReader.setLenient(true);
        ArrayList<WordType> wordTypeArrayList = new ArrayList<WordType>();
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            WordType w = new WordType();
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String elementOfWordType = jsonReader.nextName();
                if(elementOfWordType.equals("word")) {
                    w.setWord(jsonReader.nextString());
                } else if(elementOfWordType.equals("type")) {
                    w.setType(jsonReader.nextString());
                }
            }
            wordTypeArrayList.add(w);
            jsonReader.endObject();
        }
        jsonReader.endArray();*/

        /*System.out.println("The word type list of this sentence: ");
        for(WordType w : wordTypeArrayList){
            System.out.println("The word is " + w.getWord() + "    and the type is: " + w.getType());
        }
        System.out.println("--------------------------------------------------------------------");*/

        //Read the information from ArrayList<WordType>
        String tempItem = ""; //temporary tagged word
        String tempTag = "";
        double tempProb = 0.0;
        for(WordType wt: wordTypeList){
            if(!tempTag.equals("")) { //not begin of the sentence
                if("O".equals(tempTag)){ //the previous word's type is OTHER
                    if("O".equals(wt.getType())){   //the new word's type is OTHER
                        taggedText = taggedText + " " + wt.getWord();   //the word is added to the tagged text directly and the tag is NOT changed
                        tempItem = "";   //reset the tagged word
                    }else{                      //the word's type is NOT OTHER
                        tempTag = wt.getType();      //the tag is changed to the new word's type
                        tempItem = wt.getWord();     //the current tagged word is changed to the new word
                        tempProb = wt.getProbability();    //the current tagged word's probability is the new word
                    }
                }else{          //the previous word's type is NOT OTHER
                    if("O".equals(wt.getType())){    //the new word's type is OTHER
//                        taggedText = taggedText + " [[" + tempTag + " | " + String.format("%.3f",tempProb) + "]] " + "{" + tempItem + "} ";     //add the tag and the tagged word recorded previously to tagged sentence
                        taggedText = taggedText + " (tagbegin)" + tempTag + "(tagend) " +  "(wordbegin)" + tempItem + "(wordend) ";    //add the tag and tagged word without probability
                        taggedText = taggedText + " " + wt.getWord();     //add the new word to tagged text
                        tempItem = "";                                    //reset tagged word record
                        tempTag = wt.getType();                           //set the tag record to OTHER
                        tempProb = 0.0;                                   //reset the tag probability
                    }else{                           //the new word's type is NOT OTHER
                        if(tempTag.equals(wt.getType())){     //if the new word's type is same as the previous one
                            tempItem = tempItem + " " + wt.getWord();   //the word is added to the tagged word record and the tag is NOT changed
                            if(wt.getProbability() > tempProb){
                                tempProb = wt.getProbability();
                            }
                        }else{      //if the new word's type is different from the previous one
//                            taggedText = taggedText + " [[" + tempTag + " | " + String.format("%.3f",tempProb) + "]] " + "{" + tempItem + "} ";     //add the tag and the tagged word recorded previously to tagged sentence
                            taggedText = taggedText + " (tagbegin)" + tempTag + "(tagend) " +  "(wordbegin)" + tempItem + "(wordend) ";        //add the tag and tagged word without probability
                            tempItem = wt.getWord();                           //reset tagged word record and add the new word to it
                            tempTag = wt.getType();                           //set the tag record to new word's type
                            tempProb = wt.getProbability();                   //set the tag probability to new type's probability
                        }
                    }
                }
            }else{ //begin of the sentence
                if ("O".equals(wt.getType())) {  //if the first word's type is OTHER
                    taggedText = wt.getWord();   //add the first word to the tagged text directly
                    tempTag = "O";
                    tempItem = "";
                }else{                          //if the first word's type is NOT OTHER
                    tempTag = wt.getType();
                    tempItem = wt.getWord();
                    tempProb = wt.getProbability();
                }
            }

//            System.out.println("Current item: "+ tempItem);
//            System.out.println("Current tag: " + tempTag);
        }

        if(!tempItem.equals("") && !"O".equals(tempTag)){  //if the last word's type of sentence is not OTHER
            taggedText = taggedText + " (tagbegin)" + tempTag + "(tagend) " + "(wordbegin)" + tempItem + "(wordend)";     //add the last tag and the tagged word recorded previously to tagged sentence
        }

//        System.out.println("The tagged sentence is: " + taggedText);
        return taggedText;
    }

    private ArrayList<String> dividSentence(String inputText, StanfordCoreNLP pipeline){
        ArrayList<String> outputText = new ArrayList<String>();
        /*Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        props.setProperty("ner.model", "classifiers/english.all.3class.distsim.crf.ser.gz");
        props.setProperty("sutime.rules",  "sutime/defs.sutime.txt, sutime/english.holidays.sutime.txt, sutime/english.sutime.txt");
        props.setProperty("sutime.binders", "0");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        props.setProperty("ner.useSUTime", "0");*/
        Annotation document = new Annotation(inputText);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);

        ArrayList<String> names = new ArrayList<String>();
        for(CoreMap sentence: sentences) {
            //System.out.println("The sentence: " + sentence);
            outputText.add(sentence.toString());
        }

        return outputText;
    }
    private double getSimilarity(Relations r1, Relations r2, StanfordCoreNLP pipeline) {
        String document1 = r1.getSubject() + " " + r1.getRelation() + " " + r1.getArguement();
        String document2 = r2.getSubject() + " " + r2.getRelation() + " " + r2.getArguement();

        List<String> wordslist1 = getWordSet(document1, pipeline);
        List<String> wordslist2 = getWordSet(document2, pipeline);
        Set<String> words2Set = new HashSet<>();
        words2Set.addAll(wordslist2);

        Set<String> intersectionSet = new ConcurrentSkipListSet<>();
        wordslist1.parallelStream().forEach(word -> {
            if (words2Set.contains(word)) {
                intersectionSet.add(word);
            }
        });

        int intersectionSize = intersectionSet.size();

        Set<String> unionSet = new HashSet<>();
        wordslist1.forEach(word -> unionSet.add(word));
        wordslist2.forEach(word -> unionSet.add(word));

        int unionSize = unionSet.size();

        double score = intersectionSize / (double) unionSize;
        System.out.println("(" + document1 + ") and (" + document2 + ") has the similar score is: " + score);

        return score;
    }

    private List<String> getWordSet(String text, StanfordCoreNLP pipeline){
        List<String> wordslist = new ArrayList<>();;
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        List<CoreMap> words = document.get(CoreAnnotations.SentencesAnnotation.class);
        for(CoreMap word_temp: words) {
            for (CoreLabel token: word_temp.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.get(CoreAnnotations.TextAnnotation.class);  // 获取对应上面word的词元信息，即我所需要的词形还原后的单词
                wordslist.add(word);

            }
        }
        return wordslist;
    }

}
