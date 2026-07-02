module com.controlefinanceiro {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens ui to javafx.graphics, javafx.fxml;
    opens ui.home to javafx.fxml;
    opens ui.cadastros to javafx.fxml;
    opens ui.resumo to javafx.fxml;
    opens ui.components to javafx.fxml;
    opens model to javafx.base;

    exports ui;
    exports model;
    exports service;
    exports repository;
    exports db;

}
