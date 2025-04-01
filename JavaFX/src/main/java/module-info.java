module ph.dlsu.edu.ccs.stdiscm.jgang.probset3 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    opens ph.dlsu.edu.ccs.stdiscm.jgang.probset3 to javafx.fxml;
    exports ph.dlsu.edu.ccs.stdiscm.jgang.probset3;
}