package com.jpl.games;

import com.jpl.games.model.Move;
import com.jpl.games.model.Moves;
import com.jpl.games.model.Rubik;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;

/**
 *
 * @author jpereda, April 2014 - @JPeredaDnr
 */
public class RubikFX extends Application {
    
    private final BorderPane pane=new BorderPane();
    private Rubik rubik;
    
    private LocalTime time=LocalTime.now();
    private Timeline timer;
    private final StringProperty clock = new SimpleStringProperty("00:00:00");
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    
    private Button btnHover;
    
    private Moves moves=new Moves();
    
    @Override
    public void start(Stage stage) {
        /*
        Import Rubik's Cube
        */
        rubik=new Rubik();
        
        /*
        Toolbars with buttons
        */
        ToolBar tbTop=new ToolBar(new Button("U"),new Button("Ui"),new Button("F"),
                                  new Button("Fi"),new Separator(),new Button("Y"),
                                  new Button("Yi"),new Button("Z"),new Button("Zi"));
        Button bReset=new Button("Restart");
        bReset.setDisable(true);
        bReset.setOnAction(e->{
            if(moves.getNumMoves()>0){
                Action response = Dialogs.create()
                .owner(stage)
                .title("Warning Dialog")
                .masthead("Restart Game")
                .message( "You will lose all your previous movements. Do you want to continue?")
                .showConfirm();
                if(response==Dialog.Actions.YES){
                    moves.getMoves().clear();
                    rubik.doReset();
                }
            }
        });
        Button bSc=new Button("Scramble");
        bSc.setOnAction(e->{
            if(moves.getNumMoves()>0){
                Action response = Dialogs.create()
                .owner(stage)
                .title("Warning Dialog")
                .masthead("Scramble Cube")
                .message( "You will lose all your previous movements. Do you want to continue?")
                .showConfirm();
                if(response==Dialog.Actions.YES){
                    rubik.doReset();
                    doScramble();
                }
            } else {
                doScramble();
            }
        });
        ChangeListener<Number> clockLis=(ov,l,l1)->clock.set(LocalTime.ofNanoOfDay(l1.longValue()).format(fmt));
        Button bReplay=new Button("Replay");
        bReplay.setDisable(true);        
        rubik.isOnReplaying().addListener((ov,b,b1)->{
            if(b&&!b1){
                rubik.getTimestamp().removeListener(clockLis);
                if(!rubik.isSolved().get()){
                    timer.play();
                }
            }
        });
        bReplay.setOnAction(e->{
            timer.stop();
            rubik.getTimestamp().addListener(clockLis);
            doReplay();
        });
        Button bSeq=new Button("Sequence");
        bSeq.setOnAction(e->{
            String response;
            if(moves.getNumMoves()>0){
                response = Dialogs.create()
                .owner(stage)
                .title("Warning Dialog")
                .masthead("Loading a Sequence").lightweight()
                .message("Add a valid sequence of movements:\n(previous movements will be discarded)")
                .showTextInput(moves.getSequence());
            } else {
                response = Dialogs.create()
                .owner(stage)
                .title("Information Dialog")
                .masthead("Loading a Sequence").lightweight()
                .message( "Add a valid sequence of movements")
                .showTextInput();
            }
            System.out.println("r: "+response);
            if(response!=null && !response.isEmpty()){
                rubik.doReset();
                rubik.doSequence(response.trim());
            }
        });
        Label lSolved=new Label("Solved");
        lSolved.setVisible(false);
        Label lSimulated=new Label();
        lSimulated.textProperty().bind(rubik.getPreviewFace());
        tbTop.getItems().addAll(new Separator(),bReset,bSc,bReplay,bSeq,
                new Separator(),lSolved,new Separator(),lSimulated);
        pane.setTop(tbTop);
        
        ToolBar tbBottom=new ToolBar(new Button("B"),new Button("Bi"),new Button("D"),
                                     new Button("Di"),new Button("E"),new Button("Ei"));
        Label lMov=new Label();
        rubik.getCount().addListener((ov,v,v1)->{
            bReset.setDisable(moves.getNumMoves()==0);
            bReplay.setDisable(moves.getNumMoves()==0);
            lMov.setText("Movements: "+(v1.intValue()+1));
        });
        rubik.getLastRotation().addListener((ov,v,v1)->{
            if(!rubik.isOnReplaying().get() && !v1.isEmpty()){
                moves.addMove(new Move(v1, LocalTime.now().minusNanos(time.toNanoOfDay()).toNanoOfDay()));
            }
        });
        Label lTime=new Label();
        lTime.textProperty().bind(clock);
        tbBottom.getItems().addAll(new Separator(),lMov,new Separator(),lTime);
        pane.setBottom(tbBottom);
        
        ToolBar tbRight=new ToolBar(new Button("R"),new Button("Ri"),new Separator(),
                                    new Button("X"),new Button("Xi"));
        tbRight.setOrientation(Orientation.VERTICAL);
        pane.setRight(tbRight);
        ToolBar tbLeft=new ToolBar(new Button("L"),new Button("Li"),new Button("M"),
                                   new Button("Mi"),new Button("S"),new Button("Si"));
        tbLeft.setOrientation(Orientation.VERTICAL);
        pane.setLeft(tbLeft);
        
        pane.setCenter(rubik.getSubScene());
        
        pane.getChildren().stream()
            .filter(withToolbars())
            .forEach(tb->{
                ((ToolBar)tb).getItems().stream()
                    .filter(withMoveButtons())
                    .forEach(n->{
                        Button b=(Button)n;
                        b.setOnAction(e->rotateFace(b.getText()));
                        b.hoverProperty().addListener((ov,b0,b1)->updateArrow(b.getText(),b1));
                    });
            });
        
        rubik.isOnRotation().addListener((b0,b1,b2)->{
            if(b2){
                // store the button hovered 
                pane.getChildren().stream().filter(withToolbars())
                    .forEach(tb->{
                        ((ToolBar)tb).getItems().stream().filter(withMoveButtons().and(isButtonHovered()))
                            .findFirst().ifPresent(n->btnHover=(Button)n);
                    });
            } else {
                if(rubik.getPreviewFace().get().isEmpty()){
                    btnHover=null;
                } else {
                    // after rotation
                    if(btnHover!=null && !btnHover.isHover()){
                        updateArrow(btnHover.getText(), false);
                    }
                }
            }
        });
        
        // disable rest of buttons to avoid new hover events
        rubik.isOnPreview().addListener((b0, b1, b2) -> {
            final String face=rubik.getPreviewFace().get();
            pane.getChildren().stream().filter(withToolbars())
                .forEach(tb->{
                    ((ToolBar)tb).getItems().stream().filter(withMoveButtons())
                        .forEach((b)->{
                            b.setDisable(!b2 || face.isEmpty() || face.equals("V")?false:
                                    !face.equals(((Button)b).getText()));
                        });
                });
        });
        
        
        timer=new Timeline(new KeyFrame(Duration.ZERO, e->{
            clock.set(LocalTime.now().minusNanos(time.toNanoOfDay()).format(fmt));
        }),new KeyFrame(Duration.seconds(1)));
        timer.setCycleCount(Animation.INDEFINITE);
        rubik.isSolved().addListener((ov,b,b1)->{
            if(b1){
                lSolved.setVisible(true);
                timer.stop();
                moves.setTimePlay(LocalTime.now().minusNanos(time.toNanoOfDay()).toNanoOfDay());
                System.out.println(moves);
            } else {
                lSolved.setVisible(false);
            }
        });

        final Scene scene = new Scene(pane, 880, 680, true);
        scene.addEventHandler(MouseEvent.ANY, rubik.eventHandler);
        scene.cursorProperty().bind(rubik.getCursor());
        scene.setFill(Color.ALICEBLUE);
        stage.setTitle("Rubik's Cube - JavaFX3D");
        stage.setScene(scene);
        stage.show();
    }
    
    // called on button click
    private void rotateFace(final String btRot){
        pane.getChildren().stream()
            .filter(withToolbars())
            .forEach(tb->{
                ((ToolBar)tb).getItems().stream()
                    .filter(withMoveButtons().and(withButtonTextName(btRot)))
                    .findFirst().ifPresent(n->rubik.isHoveredOnClick().set(((Button)n).isHover()));
            });
        rubik.rotateFace(btRot);
    }
    
    // called on button hover
    private void updateArrow(String face, boolean hover){
        rubik.updateArrow(face,hover);
    }
    
    // called from button Scramble
    private void doScramble(){
        pane.getChildren().stream().filter(withToolbars()).forEach(setDisable(true));
        rubik.doScramble();
        rubik.isOnScrambling().addListener((ov,v,v1)->{
            if(v && !v1){
                System.out.println("scrambled!");
                pane.getChildren().stream().filter(withToolbars()).forEach(setDisable(false));
                moves=new Moves();
                time=LocalTime.now();
                timer.playFromStart();
            }
        });
    }
    
    // called from button Replay
    private void doReplay(){
        pane.getChildren().stream().filter(withToolbars()).forEach(setDisable(true));
        rubik.doReplay(moves.getMoves());
        rubik.isOnReplaying().addListener((ov,v,v1)->{
            if(v && !v1){
                System.out.println("replayed!");
                pane.getChildren().stream().filter(withToolbars()).forEach(setDisable(false));
            }
        });
    }
    
    // some predicates for readability
    private static Predicate<Node> withToolbars(){
        return n -> (n instanceof ToolBar);
    }
    private static Predicate<Node> withMoveButtons(){
        return n -> (n instanceof Button) && ((Button)n).getText().length()<=2;
    }
    private static Predicate<Node> withButtonTextName(String text){
        return n -> ((Button)n).getText().equals(text);
    }
    private static Predicate<Node> isButtonHovered(){
        return n -> ((Button)n).isHover();
    }
    private static Consumer<Node> setDisable(boolean disable){
        return n -> n.setDisable(disable);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
