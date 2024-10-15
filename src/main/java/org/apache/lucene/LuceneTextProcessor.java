
/*


package org.apache.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;

public class LuceneTextProcessor {

    private Analyzer analyzer;
    private FSDirectory indexDirectory;
    private HashMap<String, HashSet<String>> relevanceJudgments;

    public LuceneTextProcessor(String indexPath) throws IOException {
        this.analyzer = new StandardAnalyzer();
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
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(".I")) {
                    if (docContent.length() > 0) {
                        indexDoc(writer, docId, docContent.toString());
                        docContent.setLength(0);  // Clear the content buffer
                    }
                    docId = Integer.parseInt(line.split(" ")[1].trim());
                } else if (line.startsWith(".W")) {
                    continue;
                } else {
                    docContent.append(line).append("\n");
                }
            }
            // Index the last document
            if (docContent.length() > 0) {
                indexDoc(writer, docId, docContent.toString());
            }
        }
        writer.commit();
        writer.close();
    }

    private void indexDoc(IndexWriter writer, int docId, String content) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("id", String.valueOf(docId), Field.Store.YES));
        doc.add(new TextField("content", content, Field.Store.YES));
        writer.addDocument(doc);
    }

    // Method to read relevance judgments from the cranqrel file
    public void readRelevanceJudgments(String relevanceFilePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(relevanceFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");  // Split by whitespace
                if (parts.length == 3) {
                    String queryId = parts[0];
                    String docId = parts[2];

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

    // Method to sanitize query for wildcards and search the indexed documents
    public void searchDocuments(String queryString) throws ParseException, IOException {
       // queryString = queryString.replaceAll("^[*?]+", "");
        queryString = escapeSpecialCharacters(queryString.trim());// Remove leading wildcards
        try (DirectoryReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse(queryString);
            TopDocs results = searcher.search(query, 10);

            // Store results for evaluation
            System.out.println("Search Results for Query: " + queryString);
            for (ScoreDoc scoreDoc : results.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                System.out.println("Document ID: " + doc.get("id"));
            }
        }
    }

    // Method to read queries from a file and search the indexed documents
    public void readQueriesAndSearch(String queryFilePath) throws IOException, ParseException {
        try (BufferedReader reader = new BufferedReader(new FileReader(queryFilePath))) {
            String line;
            StringBuilder queryBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(".I")) {
                    if (queryBuilder.length() > 0) {
                        searchDocuments(queryBuilder.toString().trim());
                        queryBuilder.setLength(0); // Clear the builder
                    }
                } else if (line.startsWith(".W")) {
                    continue;
                } else {
                    queryBuilder.append(line).append(" ");
                }
            }
            // Search the last query
            if (queryBuilder.length() > 0) {
                searchDocuments(queryBuilder.toString().trim());
            }
        }
    }

    public static void main(String[] args) {
        String indexPath = "C:/Users/risha/Downloads/Nikita/IR&WS/Result";  // Change to your desired index directory
        String cranfieldDocsPath = "C:/Users/risha/Downloads/Nikita/IR&WS/cran/cran.all.1400";  // Change to the path of your document file
        String queryFilePath = "C:/Users/risha/Downloads/Nikita/IR&WS/cran/cran.qry"; // Change to the path of your query file
        String relevanceFilePath = "C:/Users/risha/Downloads/Nikita/IR&WS/cran/cranqrel"; // Change to the path of your relevance file

        try {
            LuceneTextProcessor processor = new LuceneTextProcessor(indexPath);
            processor.indexDocuments(cranfieldDocsPath);
            System.out.println("Indexing completed.");

            // Read relevance judgments
            processor.readRelevanceJudgments(relevanceFilePath);
            System.out.println("Relevance judgments loaded.");

            // Read queries from the file and search
            processor.readQueriesAndSearch(queryFilePath);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}*/

package org.apache.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class LuceneTextProcessor {

    private Analyzer analyzer;
    private FSDirectory indexDirectory;
    private HashMap<String, HashSet<String>> relevanceJudgments;

    public LuceneTextProcessor(String indexPath) throws IOException {
        this.analyzer = new StandardAnalyzer();
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
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(".I")) {
                    if (docContent.length() > 0) {
                        indexDoc(writer, docId, docContent.toString());
                        docContent.setLength(0);  // Clear the content buffer
                    }
                    docId = Integer.parseInt(line.split(" ")[1].trim());
                } else if (line.startsWith(".W")) {
                    continue;
                } else {
                    docContent.append(line).append("\n");
                }
            }
            // Index the last document
            if (docContent.length() > 0) {
                indexDoc(writer, docId, docContent.toString());
            }
        }
        writer.commit();
        writer.close();
    }

    private void indexDoc(IndexWriter writer, int docId, String content) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("id", String.valueOf(docId), Field.Store.YES));
        doc.add(new TextField("content", content, Field.Store.YES));
        writer.addDocument(doc);
    }

    // Method to read relevance judgments from the cranqrel file
    public void readRelevanceJudgments(String relevanceFilePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(relevanceFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");  // Split by whitespace
                if (parts.length == 3) {
                    String queryId = parts[0];
                    String docId = parts[2];

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

    // Method to sanitize query for wildcards and search the indexed documents
    public TopDocs searchDocuments(String queryString) throws ParseException, IOException {
        queryString = escapeSpecialCharacters(queryString.trim()); // Remove leading wildcards
        try (DirectoryReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse(queryString);
            return searcher.search(query, 10);
        }
    }

    // Method to evaluate search results based on relevance judgments
    public double evaluatePrecision(String queryId, TopDocs results) throws IOException {
        HashSet<String> relevantDocs = relevanceJudgments.get(queryId);
        if (relevantDocs == null || relevantDocs.isEmpty()) {
            return 0.0;
        }

        int relevantRetrieved = 0;
        double precisionSum = 0.0;

        for (int i = 0; i < results.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = results.scoreDocs[i];
            Document doc = DirectoryReader.open(indexDirectory).document(scoreDoc.doc);
            String docId = doc.get("id");

            if (relevantDocs.contains(docId)) {
                relevantRetrieved++;
                precisionSum += (double) relevantRetrieved / (i + 1);
            }
        }
        return precisionSum / relevantDocs.size();
    }

    public double calculateMAP(HashMap<String, String> queries) throws IOException, ParseException {
        double totalAP = 0.0;
        for (Map.Entry<String, String> entry : queries.entrySet()) {
            String queryId = entry.getKey();
            TopDocs results = searchDocuments(entry.getValue());
            double avgPrecision = evaluatePrecision(queryId, results);
            totalAP += avgPrecision;
        }
        return totalAP / queries.size();  // Mean Average Precision
    }

    // Method to read queries from a file and search the indexed documents
    public void readQueriesAndEvaluate(String queryFilePath) throws IOException, ParseException {
        HashMap<String, String> queries = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(queryFilePath))) {
            String line;
            StringBuilder queryBuilder = new StringBuilder();
            String currentQueryId = "";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(".I")) {
                    if (queryBuilder.length() > 0 && !currentQueryId.isEmpty()) {
                        queries.put(currentQueryId, queryBuilder.toString().trim());
                        queryBuilder.setLength(0);  // Clear query buffer
                    }
                    currentQueryId = line.split(" ")[1].trim();
                } else if (line.startsWith(".W")) {
                    continue;
                } else {
                    queryBuilder.append(line).append(" ");
                }
            }
            // Add last query
            if (queryBuilder.length() > 0 && !currentQueryId.isEmpty()) {
                queries.put(currentQueryId, queryBuilder.toString().trim());
            }

            // Evaluate MAP for all queries
            double map = calculateMAP(queries);
            System.out.println("Mean Average Precision (MAP): " + map);
        }
    }

    public static void main(String[] args) {
        String indexPath = "C:/Users/risha/Downloads/Nikita/IR&WS/Result";  // Change to your desired index directory
        String cranfieldDocsPath = "C:/Users/risha/Downloads/Nikita/IR&WS/cran/cran.all.1400";  // Change to the path of your document file
        String queryFilePath = "C:/Users/risha/Downloads/Nikita/IR&WS/cran/cran.qry"; // Change to the path of your query file
        String relevanceFilePath = "C:/Users/risha/Downloads/Nikita/IR&WS/cran/cranqrel"; // Change to the path of your relevance file

        try {
            LuceneTextProcessor processor = new LuceneTextProcessor(indexPath);
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

