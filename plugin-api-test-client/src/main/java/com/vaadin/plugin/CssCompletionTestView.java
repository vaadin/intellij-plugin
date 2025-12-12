package com.vaadin.plugin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("")
public class CssCompletionTestView extends Div {
    public CssCompletionTestView() {
        var success = new Div();
        success.setText("Success message");
        success.addClassNames("text-success", "bg-success");
        setClassName("bg-error");

        var error = new Div();
        error.setText("Error message");
        error.setClassName("text-error bg-error");
    }
}
