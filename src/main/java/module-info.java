module com.mycareer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires com.google.gson;    // JSON do login Google
    requires java.net.http;      // chamadas HTTPS ao Google
    requires jdk.httpserver;     // recebe o retorno do navegador em 127.0.0.1

    opens ui to javafx.graphics, javafx.fxml;
    opens ui.home to javafx.fxml;
    opens ui.missoes to javafx.fxml;
    opens ui.financas to javafx.fxml;
    opens ui.financas.cadastros to javafx.fxml;
    opens ui.financas.resumo to javafx.fxml;
    opens ui.agenda to javafx.fxml;
    opens ui.estoque to javafx.fxml;
    opens ui.components to javafx.fxml;
    opens model to javafx.base;

    exports ui;
    exports model;
    exports service;
    exports repository;
    exports db;

}
