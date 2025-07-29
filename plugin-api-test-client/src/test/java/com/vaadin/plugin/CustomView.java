package com.vaadin.plugin;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * A simple Vaadin view that can be used for testing get routes tool within this project.
 * This view is mapped to the route "custom".
 */
@Route(value = "custom", autoLayout = false)
public class CustomView extends VerticalLayout {
}
