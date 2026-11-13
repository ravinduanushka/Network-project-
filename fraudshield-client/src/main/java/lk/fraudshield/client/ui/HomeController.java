package lk.fraudshield.client.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;

public class HomeController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private ScrollPane mainScrollPane;

    @FXML
    private VBox scrollContent;

    @FXML
    private VBox heroSection;

    @FXML
    private VBox accountsSection;

    @FXML
    private HBox cardsSection;

    @FXML
    private HBox transfersSection;

    @FXML
    private VBox securitySection;

    @FXML
    private VBox footerSection;

    // Header navigation links
    @FXML
    private Label navAccounts;

    @FXML
    private Label navCards;

    @FXML
    private Label navTransfers;

    @FXML
    private Label navSecurity;

    // Accounts section animated cards
    @FXML
    private VBox accountCard1;

    @FXML
    private VBox accountCard2;

    @FXML
    private VBox accountCard3;

    // Cards section animated parts
    @FXML
    private Pane cardsLeftPane;

    @FXML
    private VBox cardsRightPane;

    // Transfers section animated parts
    @FXML
    private VBox transfersLeftBox;

    @FXML
    private VBox transfersRightCard;

    // Security section animated parts
    @FXML
    private GridPane securityLeftGrid;

    @FXML
    private VBox secCard1;

    @FXML
    private VBox secCard2;

    @FXML
    private VBox secCard3;

    @FXML
    private VBox secCard4;

    @FXML
    private VBox secCard5;

    @FXML
    private VBox secCard6;

    @FXML
    private VBox liveMonitorCard;

    // Buttons
    @FXML
    private Button getStartedBtn;

    @FXML
    private Button signUpBtn;

    @FXML
    private Button openAccountBtn;

    @FXML
    private Button startSavingBtn;

    @FXML
    private Button openDepositBtn;

    @FXML
    private Button sendMoneyBtn;

    @FXML
    private Button subscribeBtn;

    @FXML
    private VBox fraudToastBox;

    // Interactive controls in Cards section
    @FXML
    private Label spendingLimitLabel;

    @FXML
    private Slider spendingSlider;

    // State flags for scroll triggers
    private boolean accountsAnimated = false;
    private boolean cardsAnimated = false;
    private boolean transfersAnimated = false;
    private boolean securityAnimated = false;

    @FXML
    public void initialize() {
        if (spendingSlider != null && spendingLimitLabel != null) {
            spendingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                spendingLimitLabel.setText(String.format("Current limit: $%,.0f", newVal.doubleValue()));
            });
        }

        // Apply smooth hover popup transitions to all buttons
        applyButtonHoverEffect(signUpBtn);
        applyButtonHoverEffect(getStartedBtn);
        applyButtonHoverEffect(openAccountBtn);
        applyButtonHoverEffect(startSavingBtn);
        applyButtonHoverEffect(openDepositBtn);
        applyButtonHoverEffect(sendMoneyBtn);
        applyButtonHoverEffect(subscribeBtn);

        // Apply interactive popup hover effect to header navigation items
        applyNavHoverEffect(navAccounts);
        applyNavHoverEffect(navCards);
        applyNavHoverEffect(navTransfers);
        applyNavHoverEffect(navSecurity);

        // Apply subtle hover popup to cards
        applyCardHoverEffect(accountCard1);
        applyCardHoverEffect(accountCard2);
        applyCardHoverEffect(accountCard3);
        applyCardHoverEffect(transfersRightCard);
        applyCardHoverEffect(liveMonitorCard);
        applyCardHoverEffect(secCard1);
        applyCardHoverEffect(secCard2);
        applyCardHoverEffect(secCard3);
        applyCardHoverEffect(secCard4);
        applyCardHoverEffect(secCard5);
        applyCardHoverEffect(secCard6);

        // Scroll listener to trigger entrance animations dynamically as user scrolls down and up
        if (mainScrollPane != null) {
            mainScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                double val = newVal.doubleValue();

                if (val > 0.08 && !accountsAnimated) {
                    accountsAnimated = true;
                    playAccountsEntrance();
                } else if (val < 0.03) {
                    accountsAnimated = false;
                }

                if (val > 0.28 && !cardsAnimated) {
                    cardsAnimated = true;
                    playCardsEntrance();
                } else if (val < 0.16) {
                    cardsAnimated = false;
                }

                if (val > 0.48 && !transfersAnimated) {
                    transfersAnimated = true;
                    playTransfersEntrance();
                } else if (val < 0.36) {
                    transfersAnimated = false;
                }

                if (val > 0.68 && !securityAnimated) {
                    securityAnimated = true;
                    playSecurityEntrance();
                } else if (val < 0.54) {
                    securityAnimated = false;
                }
            });
        }

        // Trigger initial animation for visible cards on application start
        Platform.runLater(this::playAccountsEntrance);
    }

    /**
     * Smooth hover popup transition for buttons: expands slightly (1.06x) on hover with springy ease.
     */
    private void applyButtonHoverEffect(Button button) {
        if (button == null) return;
        button.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), button);
            st.setToX(1.06);
            st.setToY(1.06);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });
        button.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });
        button.setOnMousePressed(e -> {
            button.setScaleX(0.96);
            button.setScaleY(0.96);
        });
        button.setOnMouseReleased(e -> {
            button.setScaleX(1.06);
            button.setScaleY(1.06);
        });
    }

    /**
     * Interactive popup hover effect for header navigation links:
     * Scales up slightly (1.10x) with springy ease and subtle translucent pill background on hover.
     */
    private void applyNavHoverEffect(Node navItem) {
        if (navItem == null) return;
        navItem.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), navItem);
            st.setToX(1.10);
            st.setToY(1.10);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
            navItem.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14.5px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 12; -fx-background-radius: 14; -fx-background-color: rgba(255, 255, 255, 0.18);");
        });
        navItem.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), navItem);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
            navItem.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14.5px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 12; -fx-background-radius: 14; -fx-background-color: transparent;");
        });
        navItem.setOnMousePressed(e -> {
            navItem.setScaleX(0.95);
            navItem.setScaleY(0.95);
        });
        navItem.setOnMouseReleased(e -> {
            navItem.setScaleX(1.10);
            navItem.setScaleY(1.10);
        });
    }

    /**
     * Subtle upward lift hover effect for main cards.
     */
    private void applyCardHoverEffect(Node card) {
        if (card == null) return;
        card.setOnMouseEntered(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), card);
            tt.setToY(-5.0);
            tt.setInterpolator(Interpolator.EASE_OUT);
            tt.play();
        });
        card.setOnMouseExited(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), card);
            tt.setToY(0.0);
            tt.setInterpolator(Interpolator.EASE_OUT);
            tt.play();
        });
    }

    /**
     * Accounts Section Entrance Transition:
     * Card 1 arrives from Left, Card 2 arrives from Bottom, Card 3 arrives from Right into correct places.
     */
    public void playAccountsEntrance() {
        if (accountCard1 != null && accountCard2 != null && accountCard3 != null) {
            accountCard1.setTranslateX(-160.0);
            accountCard1.setOpacity(0.0);
            TranslateTransition tt1 = new TranslateTransition(Duration.millis(750), accountCard1);
            tt1.setToX(0.0);
            tt1.setInterpolator(Interpolator.EASE_OUT);
            FadeTransition ft1 = new FadeTransition(Duration.millis(750), accountCard1);
            ft1.setToValue(1.0);

            accountCard2.setTranslateY(80.0);
            accountCard2.setOpacity(0.0);
            TranslateTransition tt2 = new TranslateTransition(Duration.millis(750), accountCard2);
            tt2.setToY(0.0);
            tt2.setInterpolator(Interpolator.EASE_OUT);
            FadeTransition ft2 = new FadeTransition(Duration.millis(750), accountCard2);
            ft2.setToValue(1.0);

            accountCard3.setTranslateX(160.0);
            accountCard3.setOpacity(0.0);
            TranslateTransition tt3 = new TranslateTransition(Duration.millis(750), accountCard3);
            tt3.setToX(0.0);
            tt3.setInterpolator(Interpolator.EASE_OUT);
            FadeTransition ft3 = new FadeTransition(Duration.millis(750), accountCard3);
            ft3.setToValue(1.0);

            new ParallelTransition(tt1, ft1, tt2, ft2, tt3, ft3).play();
        }
    }

    /**
     * Cards Section Entrance Transition: Left card arrives from Left, Right panel arrives from Right.
     */
    public void playCardsEntrance() {
        if (cardsLeftPane != null && cardsRightPane != null) {
            cardsLeftPane.setTranslateX(-150.0);
            cardsLeftPane.setOpacity(0.0);
            TranslateTransition tt1 = new TranslateTransition(Duration.millis(750), cardsLeftPane);
            tt1.setToX(0.0);
            tt1.setInterpolator(Interpolator.EASE_OUT);
            FadeTransition ft1 = new FadeTransition(Duration.millis(750), cardsLeftPane);
            ft1.setToValue(1.0);

            cardsRightPane.setTranslateX(150.0);
            cardsRightPane.setOpacity(0.0);
            TranslateTransition tt2 = new TranslateTransition(Duration.millis(750), cardsRightPane);
            tt2.setToX(0.0);
            tt2.setInterpolator(Interpolator.EASE_OUT);
            FadeTransition ft2 = new FadeTransition(Duration.millis(750), cardsRightPane);
            ft2.setToValue(1.0);

            new ParallelTransition(tt1, ft1, tt2, ft2).play();
        }
    }

    /**
     * Transfers Section Entrance Transition: Left steps arrive from Left, Right card arrives from Right.
     */
    public void playTransfersEntrance() {
        if (transfersLeftBox != null && transfersRightCard != null) {
            transfersLeftBox.setTranslateX(-150.0);
            transfersLeftBox.setOpacity(0.0);
            TranslateTransition tt1 = new TranslateTransition(Duration.millis(750), transfersLeftBox);
            tt1.setToX(0.0);
            tt1.setInterpolator(Interpolator.EASE_OUT);
            FadeTransition ft1 = new FadeTransition(Duration.millis(750), transfersLeftBox);
            ft1.setToValue(1.0);

            transfersRightCard.setTranslateX(150.0);
            transfersRightCard.setOpacity(0.0);
            TranslateTransition tt2 = new TranslateTransition(Duration.millis(750), transfersRightCard);
            tt2.setToX(0.0);
            tt2.setInterpolator(Interpolator.EASE_OUT);
            FadeTransition ft2 = new FadeTransition(Duration.millis(750), transfersRightCard);
            ft2.setToValue(1.0);

            new ParallelTransition(tt1, ft1, tt2, ft2).play();
        }
    }

    /**
     * Security Section Entrance Transition:
     * Left 6 feature cards arrive from Left, Right Live monitor arrives from Right into correct places.
     */
    public void playSecurityEntrance() {
        if (securityLeftGrid != null && liveMonitorCard != null) {
            securityLeftGrid.setTranslateX(-170.0);
            securityLeftGrid.setOpacity(0.0);
            TranslateTransition tt1 = new TranslateTransition(Duration.millis(750), securityLeftGrid);
            tt1.setToX(0.0);
            tt1.setInterpolator(Interpolator.EASE_OUT);
            FadeTransition ft1 = new FadeTransition(Duration.millis(750), securityLeftGrid);
            ft1.setToValue(1.0);

            liveMonitorCard.setTranslateX(170.0);
            liveMonitorCard.setOpacity(0.0);
            TranslateTransition tt2 = new TranslateTransition(Duration.millis(750), liveMonitorCard);
            tt2.setToX(0.0);
            tt2.setInterpolator(Interpolator.EASE_OUT);
            FadeTransition ft2 = new FadeTransition(Duration.millis(750), liveMonitorCard);
            ft2.setToValue(1.0);

            new ParallelTransition(tt1, ft1, tt2, ft2).play();
        }
    }

    @FXML
    public void handleGetStarted() {
        navigateToLogin();
    }

    @FXML
    public void handleSignUp() {
        navigateToRegister();
    }

    @FXML
    private void scrollToHero() {
        if (mainScrollPane != null) {
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(450),
                            new KeyValue(mainScrollPane.vvalueProperty(), 0.0, Interpolator.EASE_BOTH))
            );
            timeline.play();
        }
    }

    @FXML
    private void scrollToAccounts() {
        scrollToNode(accountsSection);
        playAccountsEntrance();
    }

    @FXML
    private void scrollToCards() {
        scrollToNode(cardsSection);
        playCardsEntrance();
    }

    @FXML
    private void scrollToTransfers() {
        scrollToNode(transfersSection);
        playTransfersEntrance();
    }

    @FXML
    private void scrollToSecurity() {
        if (securitySection != null) {
            scrollToNode(securitySection);
            playSecurityEntrance();
        } else if (footerSection != null) {
            scrollToNode(footerSection);
        }
    }

    private void scrollToNode(Node target) {
        if (mainScrollPane == null || target == null || mainScrollPane.getContent() == null) {
            return;
        }

        Platform.runLater(() -> {
            try {
                double contentHeight = mainScrollPane.getContent().getBoundsInLocal().getHeight();
                double viewportHeight = mainScrollPane.getViewportBounds().getHeight();
                double targetY = target.getBoundsInParent().getMinY();

                double scrollableRange = contentHeight - viewportHeight;
                if (scrollableRange <= 0) return;

                double vValue = targetY / scrollableRange;
                vValue = Math.max(0.0, Math.min(1.0, vValue));

                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.millis(480),
                                new KeyValue(mainScrollPane.vvalueProperty(), vValue, Interpolator.EASE_BOTH))
                );
                timeline.play();
            } catch (Exception e) {
                target.requestFocus();
            }
        });
    }

    @FXML
    private void handleShieldClick() {
        if (fraudToastBox != null) {
            fraudToastBox.setStyle(fraudToastBox.getStyle() + "; -fx-scale-x: 1.08; -fx-scale-y: 1.08;");
            new Thread(() -> {
                try {
                    Thread.sleep(300);
                    Platform.runLater(() -> {
                        fraudToastBox.setStyle(fraudToastBox.getStyle().replace("; -fx-scale-x: 1.08; -fx-scale-y: 1.08;", ""));
                    });
                } catch (InterruptedException ignored) {}
            }).start();
        }
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(root, 1180, 750));
                stage.setTitle("FraudShield — Sign In");
                stage.show();
            } else {
                System.err.println("[WARN] Could not find stage for Login navigation");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to navigate to login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent root = loader.load();
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(root, 1180, 750));
                stage.setTitle("FraudShield — Create a Secure Banking Account");
                stage.show();
            } else {
                System.err.println("[WARN] Could not find stage for Register navigation");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to navigate to register: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Stage getStage() {
        if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
            return (Stage) rootPane.getScene().getWindow();
        }
        if (mainScrollPane != null && mainScrollPane.getScene() != null && mainScrollPane.getScene().getWindow() != null) {
            return (Stage) mainScrollPane.getScene().getWindow();
        }
        if (signUpBtn != null && signUpBtn.getScene() != null && signUpBtn.getScene().getWindow() != null) {
            return (Stage) signUpBtn.getScene().getWindow();
        }
        if (getStartedBtn != null && getStartedBtn.getScene() != null && getStartedBtn.getScene().getWindow() != null) {
            return (Stage) getStartedBtn.getScene().getWindow();
        }
        // Fallback: iterate over active showing windows
        for (Window window : Window.getWindows()) {
            if (window instanceof Stage && window.isShowing()) {
                return (Stage) window;
            }
        }
        return null;
    }
}
