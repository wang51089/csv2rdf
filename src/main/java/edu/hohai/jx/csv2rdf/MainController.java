package edu.hohai.jx.csv2rdf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import edu.hohai.jx.csv2rdf.constants.Result;
import edu.hohai.jx.csv2rdf.services.CSVParser;
import edu.hohai.jx.csv2rdf.services.RDFConverter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.openrdf.repository.RepositoryException;

import java.io.*;
import java.util.Iterator;

/**
 * Created by wjh on 2016/8/21.
 */
public class MainController {

    @FXML private TextField load;

    @FXML private TextArea preview;

    @FXML private TreeView metaTree;

    @FXML private TextArea turtle;

    private boolean isCsvFile = false;

    private ObjectNode metaRootObject;

    private ObjectNode annotatedTables;

    @FXML protected void handleSelectButtonAction(ActionEvent event) {
        preview.setText("");
        Stage stage = Stage.class.cast(Control.class.cast(event.getSource()).getScene().getWindow());
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file");
        File file = fileChooser.showOpenDialog(stage);
        String filePath = file.getPath();
        load.setText(filePath);

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filePath));
        } catch (FileNotFoundException e) {
            preview.setText("File not found: " + e);
        }
        if ( reader == null ){
            preview.setText("u didnt select file");
            return;
        }
        String line = null;
        try {
            while ((line = reader.readLine())!= null){
                preview.appendText(line + "\n");
            }
        } catch (IOException e) {
            preview.setText("error when reading file: " + e);
        }

        if( !file.getName().equals("") ){
            if(file.getName().split("\\.")[1].equals("csv")){
                isCsvFile = true;
            }
        }
    }

    @FXML protected void handleLoadButtonAction(ActionEvent event) {
        if( isCsvFile ){
            String fileString = load.getText().trim();

            if( fileString!=null && !fileString.equals("") ){


                Result result = null;
                try {
                    result = new CSVParser().parseTabularData(fileString);
                } catch (Exception e) {
                    preview.setText("exception: " + e);
                }
                metaRootObject = result.getEmbeddedMetadata();
                TreeItem<String> rootItem = new TreeItem<String>("meta date");
                buildObjectTreeNode(metaRootObject, rootItem);
                metaTree.setRoot(rootItem);
            }
        }else {
            String fileString = load.getText().trim();

            if( fileString!=null && !fileString.equals("") ){
                File metaFile = new File(fileString);
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    metaRootObject = (ObjectNode) objectMapper.readTree(metaFile);
                } catch (IOException e1) {
                    preview.setText("IO exception:" + e1);
                }
                if( metaRootObject == null ){
                    preview.setText("object mapper reader meta file returns null");
                    return;
                }
                TreeItem<String> rootItem = new TreeItem<String>("meta date");
                buildObjectTreeNode(metaRootObject, rootItem);
                metaTree.setRoot(rootItem);
            }
        }

    }

    @FXML protected void handleAddButtonAction(ActionEvent event) {
    }

    @FXML protected void handleEditButtonAction(ActionEvent event) {
    }

    @FXML protected void handleDeleteButtonAction(ActionEvent event) {
    }

    @FXML protected void handleGenerateRDFButtonAction(ActionEvent event) {
        turtle.setText("");
        RDFConverter rdfConverter = new RDFConverter();
        String content = null;
        try {
            content = rdfConverter.convert(annotatedTables);
        } catch (RepositoryException e1) {
            turtle.setText("exception: " + e1);
        }
        if( content == null ){
            turtle.appendText("converter output is null");
            return;
        }
        turtle.appendText(content);
    }

    @FXML protected void handleGenerateAnnotatedTableButtonAction(ActionEvent event) {
        turtle.setText("");
        CSVParser csvParser = new CSVParser(metaRootObject);
        try {
            annotatedTables = csvParser.createTabularData();
        } catch (Exception e1) {
            turtle.setText("exception : " + e1);
        }
        try {
            displayModel();
        } catch (JsonProcessingException e1) {
            e1.printStackTrace();
        }
    }

    @FXML protected void handleSaveButtonAction(ActionEvent event) {
        Stage stage = Stage.class.cast(Control.class.cast(event.getSource()).getScene().getWindow());
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file");
        File file = fileChooser.showSaveDialog(stage);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        PrintWriter printWriter = new PrintWriter(fileOutputStream);
        String content = turtle.getText();
        printWriter.write(content);
        if (printWriter != null) {
            printWriter.close();
        }
    }

    private void displayModel() throws JsonProcessingException {
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                /*jTextArea.append("table group annotations:\n");*/
        turtle.appendText("group annotations：\n");
        Iterator<String> fieldNames = annotatedTables.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (fieldName.equals("tables")) {
                continue;
            } else {
                JsonNode jsonNode = annotatedTables.get(fieldName);
                String jsonString = jsonNode.toString();
                turtle.appendText("\t\"" + fieldName + "\"   :   " + jsonString);
                turtle.appendText("\n");
            }
        }
        turtle.appendText("\n");
        ArrayNode tables = (ArrayNode) annotatedTables.get("tables");
        for (int i = 0; i < tables.size(); i++) {
            ///////////////////////////////////////////////////////////////////////////////////////////////
                    /*jTextArea.append("table[" + (i + 1) + "] annoatations:\n");*/
            turtle.appendText("table[" + (i + 1) + "] annotations：\n");
            ObjectNode table = (ObjectNode) tables.get(i);
            fieldNames = table.fieldNames();

            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (fieldName.equals("rows") || fieldName.equals("columns")) {
                    continue;
                } else {
                    JsonNode jsonNode = table.get(fieldName);
                    String jsonString = jsonNode.toString();
                    turtle.appendText("\t\"" + fieldName + "\"   :   " + jsonString);
                    turtle.appendText("\n");
                }
            }
            turtle.appendText("\n");
            ArrayNode rows = (ArrayNode) table.get("rows");
            for (int j = 0; j < rows.size(); j++) {
                //////////////////////////////////////////////////////////////////////////////////////////////
                        /*jTextArea.append("row[" + (j + 1) + "] of table[" + (i + 1) + "] has annoatations:\n");*/
                turtle.appendText("row[" + (j + 1) + "] annotations：\n");
                ObjectNode row = (ObjectNode) rows.get(j);
                fieldNames = row.fieldNames();

                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    if (fieldName.equals("cells")) {
                        continue;
                    } else if (fieldName.equals("table")) {
                        //////////////////////////////////////////////////////////////////////////////////////
                                /*jTextArea.append("\ttable   `s value is   table[" + i + "]");*/
                        turtle.appendText("\t\"table\": table[" + i + "]");
                        turtle.appendText("\n");
                    } else if (fieldName.equals("primaryKey")) {
                        JsonNode jsonNode = row.get("primaryKey");
                        ArrayNode jsonNodes = (ArrayNode) jsonNode;
                        if (jsonNodes.size() > 0) {
                            StringBuilder jsonString = new StringBuilder();
                            for (int ii = 0; ii < jsonNodes.size(); ii++) {
                                jsonString.append(jsonNodes.get(ii).asText() + ",");
                            }
                            jsonString.deleteCharAt(jsonString.length() - 1);
                            turtle.appendText("\t\"" + fieldName + "\"   :   " + jsonString);
                            turtle.appendText("\n");
                        }
                    } else if( fieldName.equals("referencedRows") ){

                    }else {
                        JsonNode jsonNode = row.get(fieldName);
                        String jsonString = jsonNode.toString();
                        turtle.appendText("\t\"" + fieldName + "\"   :   " + jsonString );
                        turtle.appendText("\n");
                    }
                }
                turtle.appendText("\n");
            }
            ArrayNode columns = (ArrayNode) table.get("columns");
            for (int k = 0; k < columns.size(); k++) {
                        /*jTextArea.append("column[" + (k + 1) + "] of table[" + (i + 1) + "] has annoatations:\n");*/
                turtle.appendText("column[" + (k + 1) + "] annotations：\n");
                ObjectNode column = (ObjectNode) columns.get(k);
                fieldNames = column.fieldNames();

                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    if (fieldName.equals("cells")) {
                        continue;
                    } else if (fieldName.equals("table")) {
                        turtle.appendText("\t\"table\": table[" + i + "]");
                        turtle.appendText("\n");
                    } else {
                        JsonNode jsonNode = column.get(fieldName);
                        String jsonString = jsonNode.toString();
                        turtle.appendText("\t\"" + fieldName + "\"   :   " + jsonString);
                        turtle.appendText("\n");
                    }
                }
                turtle.appendText("\n");
            }

            for (int m = 0; m < rows.size(); m++) {
                for (int n = 0; n < columns.size(); n++) {
                    ////////////////////////////////////////////////////////////////////////////////////////////////////
                            /*jTextArea.append("table[" + (i + 1) + "], cell[" + (m + 1) + "," + (n + 1) + "] has annotations:\n");*/
                    turtle.appendText("cell[" + (m + 1) + "," + (n + 1) + "] annotations：\n");
                    ObjectNode cell = (ObjectNode) rows.get(m).get("cells").get(n);
                    Iterator<String> fieldNames1 = cell.fieldNames();

                    while (fieldNames1.hasNext()) {
                        String fieldName = fieldNames1.next();
                        if (fieldName.equals("table")) {
                            turtle.appendText("\t\"table\": table[" + i + "]");
                            turtle.appendText("\n");
                        } else if (fieldName.equals("row")) {
                            ObjectNode row = (ObjectNode) cell.get("row");
                            int rowNum = row.get("number").asInt();
                            turtle.appendText("\t\"row\": row[" + rowNum + "]");
                            turtle.appendText("\n");
                        } else if (fieldName.equals("column")) {
                            ObjectNode column = (ObjectNode) cell.get("column");
                            int columnNum = column.get("number").asInt();
                            turtle.appendText("\t\"column\": column[" + columnNum + "]");
                            turtle.appendText("\n");
                        } else {
                            JsonNode jsonNode = cell.get(fieldName);
                            String jsonString = jsonNode.toString();
                            turtle.appendText("\t\"" + fieldName + "\"   :   " + jsonString);
                            turtle.appendText("\n");
                        }
                    }
                    turtle.appendText("\n");
                }
            }
        }
    }


    void buildObjectTreeNode(ObjectNode objectNode , TreeItem<String> parentItem) {
        Iterator<String> propertyNames = objectNode.fieldNames();
        while (propertyNames.hasNext()) {
            String fieldName = propertyNames.next();
            JsonNode fieldNode = objectNode.get(fieldName);
            if (fieldNode.getNodeType() == JsonNodeType.OBJECT) {
                TreeItem<String> child = new TreeItem<String>(fieldName + "   >   (object)");
                parentItem.getChildren().add(child);
                buildObjectTreeNode((ObjectNode) fieldNode, child);
            } else if (fieldNode.getNodeType() == JsonNodeType.ARRAY) {
                TreeItem<String> child = new TreeItem<String>(fieldName + "   >   (array)");
                parentItem.getChildren().add(child);
                buildArrayTreeNode((ArrayNode) fieldNode, child);
            } else {
                TreeItem<String> newChild = new TreeItem<String>(fieldName + "   >   " + fieldNode.asText());
                parentItem.getChildren().add(newChild);
            }
        }
    }

    private void buildArrayTreeNode(ArrayNode arrayNode, TreeItem<String> parentItem) {
        Iterator<JsonNode> elements = arrayNode.elements();
        int i = 0;
        while (elements.hasNext()) {
            JsonNode jsonNode = elements.next();
            if (jsonNode.getNodeType() == JsonNodeType.OBJECT) {
                TreeItem<String> newChild = new TreeItem<String>(i + "   >   (object)");
                parentItem.getChildren().add(newChild);
                buildObjectTreeNode((ObjectNode) jsonNode, newChild);
            } else if (jsonNode.getNodeType() == JsonNodeType.ARRAY) {
                TreeItem<String> newChild = new TreeItem<String>(i + "   >   (array)");
                parentItem.getChildren().add(newChild);
                buildArrayTreeNode((ArrayNode) jsonNode, newChild);
            } else {
                TreeItem<String> newChild = new TreeItem<String>(i + "   >   " + jsonNode.asText());
                parentItem.getChildren().add(newChild);
            }
            i++;
        }
    }

    public String jsonFormatter(String uglyJSONString) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(uglyJSONString);
        String prettyJsonString = gson.toJson(je);
        return prettyJsonString;
    }
}
