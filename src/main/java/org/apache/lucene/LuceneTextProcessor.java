package org.apache.lucene;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;
import java.io.File;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.Tokenizer;


import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class LuceneTextProcessor {

    private Analyzer analyzer;
    private FSDirectory indexDirectory;
    private HashMap<String, HashSet<String>> relevanceJudgments;

    public class CustomAnalyzer extends Analyzer {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            // Tokenizer - Standard Tokenizer for tokenization
            Tokenizer source = new StandardTokenizer();

            // Lowercase Filter - convert tokens to lowercase
            TokenStream tokenStream = new LowerCaseFilter(source);

            // Stop Filter - remove common stop words
            tokenStream = new StopFilter(tokenStream, EnglishAnalyzer.getDefaultStopSet());

            // Porter Stem Filter - apply stemming
            tokenStream = new PorterStemFilter(tokenStream);

            return new TokenStreamComponents(source, tokenStream);
        }
    }

    public LuceneTextProcessor(String indexPath) throws IOException {
        this.analyzer = new CustomAnalyzer();
        this.indexDirectory = FSDirectory.open(Paths.get(indexPath));
        this.relevanceJudgments = new HashMap<>();
    }

    // Method to index Cranfield documents
    public void indexDocuments(String filePath) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(indexDirectory, config);

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            StringBuilder docContent = new StringBuilder();
            int docId = 0;
            String title = ""; // Initialize title variable
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(".I")) {
                    // If there's content from a previous document, index it
                    if (docContent.length() > 0) {
                        indexDoc(writer, docId, title, docContent.toString());
                        docContent.setLength(0); // Clear the content buffer
                        title = ""; // Reset title for the next document
                    }
                    // Read the new document ID
                    docId = Integer.parseInt(line.split(" ")[1].trim());
                    System.out.println("docId: " + docId);
                } else if (line.startsWith(".T")) { // Capture title if present
                    StringBuilder titleBuilder = new StringBuilder();
                    // Read subsequent lines for the title
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith(".A") || line.startsWith(".B")) { // End of title section
                            break; // Exit loop when encountering the next marker
                        }
                        titleBuilder.append(line.trim()).append(" "); // Accumulate title lines
                    }
                    title = titleBuilder.toString().trim(); // Capture title
                    System.out.println("Title: " + title);
                } else if (line.startsWith(".W")) { // Start of the document content
                    StringBuilder contentBuilder = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith(".I")) { // End of content section and beginning of new document
                            docContent.append(contentBuilder.toString().trim()); // Append content
                            indexDoc(writer, docId, title, docContent.toString()); // Index the document
                            docContent.setLength(0); // Clear content buffer for next doc
                            docId = Integer.parseInt(line.split(" ")[1].trim()); // Read new document ID
                            System.out.println("New docId: " + docId); // Process the next docId
                            break; // Exit inner loop to process next document
                        }
                        contentBuilder.append(line.trim()).append(" "); // Accumulate content
                    }
                    if (line == null) {
                        // If end of file, index the last document
                        docContent.append(contentBuilder.toString().trim());
                        indexDoc(writer, docId, title, docContent.toString()); // Index last document
                    }
                }
            }
        }

        writer.commit();
        writer.close();
    }

    private void indexDoc(IndexWriter writer, int docId, String title, String content) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("id", String.valueOf(docId), Field.Store.YES));
        doc.add(new TextField("title", title, Field.Store.YES)); // Adding title field
        doc.add(new TextField("content", content, Field.Store.YES)); // Adding content field
        writer.addDocument(doc);
    }

    // Method to read relevance judgments from the cranqrel file
    public void readRelevanceJudgments(String relevanceFilePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(relevanceFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+"); // Split by whitespace
                if (parts.length == 3) {
                    String queryId = parts[0];
                    String docId = parts[1];

                    // Add to the relevance judgments map
                    relevanceJudgments.computeIfAbsent(queryId, k -> new HashSet<>()).add(docId);
                }
            }
        }
    }

    // Escape special characters in Lucene queries
    public String escapeSpecialCharacters(String queryString) {
        String[] specialChars = {"\\", "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", "*", "?", ":", "/"};
        for (String specialChar : specialChars) {
            queryString = queryString.replace(specialChar, "\\" + specialChar);
        }
        return queryString;
    }
    // In your search method, set the similarity to BM25
    public TopDocs searchWithBM25(String queryString) throws ParseException, IOException {
        queryString = escapeSpecialCharacters(queryString.trim());

        try (DirectoryReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            // Set BM25 as the similarity model
            searcher.setSimilarity(new BM25Similarity());

            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse(queryString);

            return searcher.search(query, 50); // Retrieve top 10 results
        }
    }

    // In your search method, set the similarity to ClassicSimilarity (TF-IDF)
    public TopDocs searchWithTFIDF(String queryString) throws ParseException, IOException {
    queryString = escapeSpecialCharacters(queryString.trim());

        try (DirectoryReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

           // Set TF-IDF (ClassicSimilarity) as the similarity model
        searcher.setSimilarity(new ClassicSimilarity());

        QueryParser parser = new QueryParser("content", analyzer);
        Query query = parser.parse(queryString);

        return searcher.search(query, 50); // Retrieve top 10 results
       }
    }


    public double evaluatePrecision(String queryId, TopDocs results) throws IOException {
        HashSet<String> relevantDocs = relevanceJudgments.get(queryId);
        if (relevantDocs == null || relevantDocs.isEmpty()) {
            return 0.0;
        }

        int relevantRetrieved = 0;
        double precisionSum = 0.0;

        System.out.println("Results for Query ID: " + queryId);
        for (int i = 0; i < results.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = results.scoreDocs[i];
            Document doc = DirectoryReader.open(indexDirectory).document(scoreDoc.doc);
            String docId = doc.get("id");
            boolean isRelevant = relevantDocs.contains(docId);

            // Print the rank, document ID, and relevance status
            System.out.println("Rank: " + (i + 1) + ", Document ID: " + docId + ", Relevant: " + isRelevant);

            if (isRelevant) {
                relevantRetrieved++;
                precisionSum += (double) relevantRetrieved / (i + 1);  // Precision at i+1
            }
        }

        double avgPrecision = precisionSum / relevantDocs.size();  // Average Precision for this query
        System.out.println("Average Precision for Query ID " + queryId + ": " + avgPrecision);
        System.out.println("=========================================");
        return avgPrecision;
    }
    private void storeTopResults_bm25(int queryId, TopDocs topDocs) throws IOException {
        String outputPath = "/opt/IRWR_Proj/Result/results.txt";
/*	File file = new File(outputPath);
        if (file.exists()) {
            // If file exists, delete it
            if (file.delete()) {
                System.out.println("Result File for BM25 deleted successfully");
            } else {
                System.out.println("Failed to delete the file for BM25");
            }
        } else {
            System.out.println("BM25 result File does not exist.");
        }
*/
	try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath, true))) { // Append mode
            for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                Document doc = DirectoryReader.open(indexDirectory).document(scoreDoc.doc);
                String docId = doc.get("id");

                // TREC_Eval format: query-id Q0 doc-id rank score run-tag
                writer.write(String.format("%d Q0 %s %d %.6f LuceneSearch\n", queryId, docId, (i + 1), scoreDoc.score));
            }
        }
    }
    private void storeTopResults_tf_idf(int queryId, TopDocs topDocs) throws IOException {
	String outputPath1 = "/opt/IRWR_Proj/Result/results1.txt";

        // Create a File object representing the file
        /*File file = new File(outputPath1);
	if (file.exists()) {
            // If file exists, delete it
            if (file.delete()) {
                System.out.println("Result File for VSM deleted successfully");
            } else {
                System.out.println("Failed to delete the file for VSM");
            }
        } else {
            System.out.println("File does not exist.");
        }*/
        //String outputPath1 = "/opt/IRWR_Proj/results1.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath1, true))) { // Append mode
            for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                Document doc = DirectoryReader.open(indexDirectory).document(scoreDoc.doc);
                String docId = doc.get("id");

                // TREC_Eval format: query-id Q0 doc-id rank score run-tag
                writer.write(String.format("%d Q0 %s %d %.6f LuceneSearch\n", queryId, docId, (i + 1), scoreDoc.score));
            }
        }
    }

    public void calculateMAP(HashMap<String, String> queries) throws IOException, ParseException {
        //double totalAP = 0.0;
        System.out.println("Starting MAP calculation...");

        for (Map.Entry<String, String> entry : queries.entrySet()) {
            String queryId = entry.getKey();
            System.out.println("Processing Query ID: " + queryId);
            System.out.println("Processing Query: " + entry.getValue());
            TopDocs bm25Results = searchWithBM25(entry.getValue());
            //TopDocs results = searchWithBM25(entry.getValue()); // Search with the query text
            storeTopResults_bm25(Integer.parseInt(queryId),bm25Results); // Store the top results
            //double avgPrecision = evaluatePrecision(queryId, bm25Results); // Evaluate precision for this query
            //totalAP += avgPrecision; // Add to total average precision
	    TopDocs tfidfResults = searchWithTFIDF(entry.getValue());
	    storeTopResults_tf_idf(Integer.parseInt(queryId), tfidfResults);
        }

        //return totalAP / queries.size();  // Mean Average Precision
    }

    public void readQueriesAndEvaluate(String queryFilePath) throws IOException, ParseException {
        HashMap<String, String> queries = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(queryFilePath))) {
            String line;
            StringBuilder queryBuilder = new StringBuilder();
            int queryIdCounter = 0; // Initialize the query ID counter
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(".I")) {
                    // If we have accumulated a query, store it with the current query ID
                    if (queryBuilder.length() > 0) {
                        String queryText = queryBuilder.toString().trim();
                        queries.put(String.valueOf(queryIdCounter), queryText); // Store the query
                        System.out.println("Query ID: " + queryIdCounter + ", Text: " + queryText); // Print Query ID and Text
                        queryBuilder.setLength(0); // Clear query buffer
                    }
                    // Increment the query ID counter
                    queryIdCounter++;
                } else if (line.startsWith(".W")) {
                    continue; // Skip lines starting with .W
                } else {
                    queryBuilder.append(line).append(" "); // Accumulate query text
                }
            }
            // Add last query if any
            if (queryBuilder.length() > 0) {
                String queryText = queryBuilder.toString().trim();
                queries.put(String.valueOf(queryIdCounter), queryText);
                System.out.println("Query ID: " + queryIdCounter + ", Text: " + queryText); // Print Query ID and Text
            }
            // Evaluate MAP for all queries
            calculateMAP(queries);
        }
    }
    private void cleanIndexDirectory(String indexPath) {
        File indexDir = new File(indexPath);
        if (indexDir.exists() && indexDir.isDirectory()) {
            for (File file : indexDir.listFiles()) {
                if (file.isFile()) {
                    if (file.delete()) {
                        System.out.println("Deleted file: " + file.getName());
                    } else {
                        System.out.println("Failed to delete file: " + file.getName());
                    }
                }
            }
        }
    }
    public static void main(String[] args) {
            String indexPath = "/opt/IRWR_Proj/output";  // Change to your desired index directory
            String cranfieldDocsPath = "/opt/IRWR_Proj/input_files/cran.all.1400";  // Change to the path of your document file
            String queryFilePath = "/opt/IRWR_Proj/input_files/cran.qry"; // Change to the path of your query file
            String relevanceFilePath = "/opt/IRWR_Proj/input_files/cranqrel"; // Change to the path of your relevance file
            String resultPath = "/opt/IRWR_Proj/Result";

       try {
            LuceneTextProcessor processor = new LuceneTextProcessor(indexPath);
            processor.cleanIndexDirectory(indexPath);
	    processor.cleanIndexDirectory(resultPath);
	    processor.indexDocuments(cranfieldDocsPath);
            System.out.println("Indexing completed.");

            // Read relevance judgments
            processor.readRelevanceJudgments(relevanceFilePath);
            System.out.println("Relevance judgments loaded.");

            // Read queries and evaluate MAP
            processor.readQueriesAndEvaluate(queryFilePath);

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}

