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
import javafx.scene.control.MenuButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import lk.fraudshield.client.util.LanguageManager;

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

    // Language Dropdown
    @FXML
    private MenuButton languageMenuBtn;

    // Hero Section
    @FXML
    private Label heroTitleLine1;

    @FXML
    private Label heroTitleLine2;

    @FXML
    private Label heroSubtitle;

    // Phone Mockup & Toast
    @FXML
    private Label phoneWelcomeLabel;

    @FXML
    private Label phoneTotalBalanceLabel;

    @FXML
    private Label phoneSendLabel;

    @FXML
    private Label phoneRequestLabel;

    @FXML
    private Label phoneShieldLabel;

    @FXML
    private Label phoneMoreLabel;

    @FXML
    private Label phoneRecentActivityLabel;

    @FXML
    private Label phoneShoppingLabel;

    @FXML
    private Label phoneBillsPaidLabel;

    @FXML
    private Label phoneTravelLabel;

    @FXML
    private Label fraudToastTitle;

    @FXML
    private Label fraudToastSub;

    // Accounts section animated cards
    @FXML
    private VBox accountCard1;

    @FXML
    private VBox accountCard2;

    @FXML
    private VBox accountCard3;

    @FXML
    private Label accountsTitle;

    @FXML
    private Label accountsSubtitle;

    @FXML
    private Label accountCard1Title;

    @FXML
    private Label accountCard1Desc;

    @FXML
    private Label accountCard1Fee;

    @FXML
    private Label accountCard1B1;

    @FXML
    private Label accountCard1B2;

    @FXML
    private Label accountCard1B3;

    @FXML
    private Label accountCard2Badge;

    @FXML
    private Label accountCard2Title;

    @FXML
    private Label accountCard2Desc;

    @FXML
    private Label accountCard2Rate;

    @FXML
    private Label accountCard2B1;

    @FXML
    private Label accountCard2B2;

    @FXML
    private Label accountCard2B3;

    @FXML
    private Label accountCard3Title;

    @FXML
    private Label accountCard3Desc;

    @FXML
    private Label accountCard3Rate;

    @FXML
    private Label accountCard3B1;

    @FXML
    private Label accountCard3B2;

    @FXML
    private Label accountCard3B3;

    // Cards section animated parts
    @FXML
    private Pane cardsLeftPane;

    @FXML
    private VBox cardsRightPane;

    @FXML
    private Label cardsTitle;

    @FXML
    private Label cardsSubtitle;

    @FXML
    private Label cardFreezeTitle;

    @FXML
    private Label cardFreezeDesc;

    @FXML
    private Label cardOnlineTitle;

    @FXML
    private Label cardOnlineDesc;

    @FXML
    private Label cardLimitTitle;

    // Transfers section animated parts
    @FXML
    private VBox transfersLeftBox;

    @FXML
    private VBox transfersRightCard;

    @FXML
    private Label transfersBadge;

    @FXML
    private Label transfersTitle;

    @FXML
    private Label transfersSubtitle;

    @FXML
    private Label step1Title;

    @FXML
    private Label step1Desc;

    @FXML
    private Label step2Title;

    @FXML
    private Label step2Desc;

    @FXML
    private Label step3Title;

    @FXML
    private Label step3Desc;

    @FXML
    private Label step4Title;

    @FXML
    private Label step4Desc;

    @FXML
    private Label previewTransferTitle;

    @FXML
    private Label previewProtectedBadge;

    @FXML
    private Label previewFromLabel;

    @FXML
    private Label previewToLabel;

    @FXML
    private Label previewAmountLabel;

    @FXML
    private Label previewVerdictLabel;

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

    @FXML
    private Label securityTitle;

    @FXML
    private Label securitySubtitle;

    @FXML
    private Label sec1Title;

    @FXML
    private Label sec1Desc;

    @FXML
    private Label sec2Title;

    @FXML
    private Label sec2Desc;

    @FXML
    private Label sec3Title;

    @FXML
    private Label sec3Desc;

    @FXML
    private Label sec4Title;

    @FXML
    private Label sec4Desc;

    @FXML
    private Label sec5Title;

    @FXML
    private Label sec5Desc;

    @FXML
    private Label sec6Title;

    @FXML
    private Label sec6Desc;

    @FXML
    private Label monitorTitle;

    @FXML
    private Label monitorLiveBadge;

    @FXML
    private Label monitorStat1Label;

    @FXML
    private Label monitorStat2Label;

    @FXML
    private Label monitorStat3Label;

    // Footer section
    @FXML
    private Label footerDesc;

    @FXML
    private Label footerBankingHeader;

    @FXML
    private Label footerCompanyHeader;

    @FXML
    private Label footerSupportHeader;

    @FXML
    private Label footerStayUpdatedHeader;

    @FXML
    private Label footerNewsletterSub;

    @FXML
    private Label footerAboutUs;

    @FXML
    private Label footerOurTeam;

    @FXML
    private Label footerCareers;

    @FXML
    private Label footerBlog;

    @FXML
    private Label footerHelpCenter;

    @FXML
    private Label footerReportFraud;

    @FXML
    private Label footerFaqs;

    @FXML
    private Label footerContactUs;

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
                if (LanguageManager.isSinhala()) {
                    spendingLimitLabel.setText(String.format("වත්මන් සීමාව: $%,.0f", newVal.doubleValue()));
                } else {
                    spendingLimitLabel.setText(String.format("Current limit: $%,.0f", newVal.doubleValue()));
                }
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

        // Initialize language state
        applyLanguage(LanguageManager.isSinhala());

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

    @FXML
    private void handleLanguageEnglish() {
        LanguageManager.setLanguage(LanguageManager.Language.ENGLISH);
        applyLanguage(false);
    }

    @FXML
    private void handleLanguageSinhala() {
        LanguageManager.setLanguage(LanguageManager.Language.SINHALA);
        applyLanguage(true);
    }

    private void applyLanguage(boolean isSinhala) {
        if (isSinhala) {
            if (languageMenuBtn != null) languageMenuBtn.setText("🌐 සිං");
            if (navAccounts != null) navAccounts.setText("ගිණුම්");
            if (navCards != null) navCards.setText("කාඩ්පත්");
            if (navTransfers != null) navTransfers.setText("මුදල් හුවමාරු");
            if (navSecurity != null) navSecurity.setText("ආරක්ෂාව");
            if (signUpBtn != null) signUpBtn.setText("ලියාපදිංචි වන්න");

            // Hero
            if (heroTitleLine1 != null) heroTitleLine1.setText("ඔබේ කාර්යබහුල ජීවිතයට");
            if (heroTitleLine2 != null) heroTitleLine2.setText("පහසු බැංකු සේවාවක්.");
            if (heroSubtitle != null) heroSubtitle.setText("ආරක්ෂිත ගිණුම්, ක්ෂණික මුදල් හුවමාරු සහ තත්‍ය කාලීන\nවංචා හඳුනාගැනීම — සියල්ල එකම යෙදුමකින්.");
            if (getStartedBtn != null) getStartedBtn.setText("ඇතුළු වන්න");

            // Phone mockup
            if (phoneWelcomeLabel != null) phoneWelcomeLabel.setText("නැවත සාදරයෙන් පිළිගනිමු");
            if (phoneTotalBalanceLabel != null) phoneTotalBalanceLabel.setText("මුළු ශේෂය");
            if (phoneSendLabel != null) phoneSendLabel.setText("යවන්න");
            if (phoneRequestLabel != null) phoneRequestLabel.setText("ඉල්ලන්න");
            if (phoneShieldLabel != null) phoneShieldLabel.setText("ආරක්ෂාව");
            if (phoneMoreLabel != null) phoneMoreLabel.setText("තවත්");
            if (phoneRecentActivityLabel != null) phoneRecentActivityLabel.setText("මෑත ගනුදෙනු");
            if (phoneShoppingLabel != null) phoneShoppingLabel.setText("සාප්පු සවාරි");
            if (phoneBillsPaidLabel != null) phoneBillsPaidLabel.setText("බිල්පත් ගෙවීම්");
            if (phoneTravelLabel != null) phoneTravelLabel.setText("සංචාරක වියදම්");
            if (fraudToastTitle != null) fraudToastTitle.setText("වංචාව වැළැක්විණි");
            if (fraudToastSub != null) fraudToastSub.setText("සැකකටයුතු ගෙවීම\nතත්‍ය කාලීනව වළක්වන ලදී");

            // Accounts
            if (accountsTitle != null) accountsTitle.setText("සෑම ඉලක්කයකටම ගැලපෙන ගිණුමක්");
            if (accountsSubtitle != null) accountsSubtitle.setText("මිනිත්තු කිහිපයකින් ගිණුමක් අරඹන්න. පළමු දිනයේ සිටම FraudShield තත්‍ය කාලීන අධීක්ෂණයෙන් ආරක්ෂා වේ.");
            if (accountCard1Title != null) accountCard1Title.setText("ජංගම ගිණුම");
            if (accountCard1Desc != null) accountCard1Desc.setText("ඔබගේ දෛනික වියදම්, වැටුප් සහ බිල්පත් ගෙවීම් සඳහා.");
            if (accountCard1Fee != null) accountCard1Fee.setText("$0 / මාසික ගාස්තු රහිතයි");
            if (accountCard1B1 != null) accountCard1B1.setText("✓  නොමිලේ ඩෙබිට් කාඩ්පතක්");
            if (accountCard1B2 != null) accountCard1B2.setText("✓  සීමාවකින් තොර දේශීය මුදල් හුවමාරු");
            if (accountCard1B3 != null) accountCard1B3.setText("✓  ක්ෂණික වියදම් දැනුම්දීම්");
            if (openAccountBtn != null) openAccountBtn.setText("ගිණුමක් අරඹන්න");

            if (accountCard2Badge != null) accountCard2Badge.setText("ජනප්‍රියයි");
            if (accountCard2Title != null) accountCard2Title.setText("ඉතුරුම් ගිණුම");
            if (accountCard2Desc != null) accountCard2Desc.setText("මාසිකව ගෙවන ඉහළ පොලියක් සමඟ ඔබේ මුදල් වර්ධනය කරගන්න.");
            if (accountCard2Rate != null) accountCard2Rate.setText("වසරකට 6.5% ක පොලියක්");
            if (accountCard2B1 != null) accountCard2B1.setText("✓  මාසික පොලී ගෙවීම");
            if (accountCard2B2 != null) accountCard2B2.setText("✓  ඉතුරුම් ඉලක්ක සහ සැලසුම්");
            if (accountCard2B3 != null) accountCard2B3.setText("✓  අවම ශේෂයක් අවශ්‍ය නැත");
            if (startSavingBtn != null) startSavingBtn.setText("ඉතිරි කිරීම අරඹන්න");

            if (accountCard3Title != null) accountCard3Title.setText("ස්ථාවර තැන්පතු");
            if (accountCard3Desc != null) accountCard3Desc.setText("ස්ථාවර කාලයකට ඉහළ සහතික කළ ප්‍රතිලාභයක් ලබා ගන්න.");
            if (accountCard3Rate != null) accountCard3Rate.setText("මාස 24 දක්වා වසරකට 9.0% පොලියක්");
            if (accountCard3B1 != null) accountCard3B1.setText("✓  මාස 3 සිට 24 දක්වා කාලසීමා");
            if (accountCard3B2 != null) accountCard3B2.setText("✓  සහතික කළ ප්‍රතිලාභ");
            if (accountCard3B3 != null) accountCard3B3.setText("✓  ස්වයංක්‍රීයව අලුත් කිරීමේ හැකියාව");
            if (openDepositBtn != null) openDepositBtn.setText("තැන්පතුවක් අරඹන්න");

            // Cards
            if (cardsTitle != null) cardsTitle.setText("ඔබේ කාඩ්පත, සම්පූර්ණයෙන්ම\nඔබේ පාලනය යටතේ");
            if (cardsSubtitle != null) cardsSubtitle.setText("එක ක්ලික් එකකින් පාලනය කළ හැකි භෞතික සහ ඩිජිටල් ඩෙබිට් කාඩ්පත්. පහතින් අත්හදා බලන්න.");
            if (cardFreezeTitle != null) cardFreezeTitle.setText("කාඩ්පත අක්‍රිය කරන්න (Freeze)");
            if (cardFreezeDesc != null) cardFreezeDesc.setText("කාඩ්පත නැතිවුණාද? ක්ෂණිකව Freeze කරන්න, ඕනෑම වේලාවක Unfreeze කරන්න.");
            if (cardOnlineTitle != null) cardOnlineTitle.setText("මාර්ගගත ගෙවීම් (Online Payments)");
            if (cardOnlineDesc != null) cardOnlineDesc.setText("අන්තර්ජාල හා විදේශීය ගෙවීම් සක්‍රිය හෝ අක්‍රිය කරන්න.");
            if (cardLimitTitle != null) cardLimitTitle.setText("දෛනික වියදම් සීමාව");
            if (spendingSlider != null && spendingLimitLabel != null) {
                spendingLimitLabel.setText(String.format("වත්මන් සීමාව: $%,.0f", spendingSlider.getValue()));
            }

            // Transfers
            if (transfersBadge != null) transfersBadge.setText("මුදල් හුවමාරු");
            if (transfersTitle != null) transfersTitle.setText("ක්ෂණිකව සහ ආරක්ෂිතව —\nමුදල් යවන්න");
            if (transfersSubtitle != null) transfersSubtitle.setText("සෑම මුදල් හුවමාරුවක්ම පිටවීමට පෙර වංචා අවදානම් පරීක්ෂාවට ලක් වේ. තත්පර කිහිපයකින් ආරක්ෂිතව යැවේ.");
            if (step1Title != null) step1Title.setText("ලබන්නා තෝරන්න");
            if (step1Desc != null) step1Desc.setText("සුරකින ලද සම්බන්ධතාවයක් තෝරන්න හෝ ගිණුම් අංකය ඇතුළත් කරන්න.");
            if (step2Title != null) step2Title.setText("මුදල ඇතුළත් කරන්න");
            if (step2Desc != null) step2Desc.setText("හේතුව මතක තබා ගැනීමට සටහනක් එක් කරන්න.");
            if (step3Title != null) step3Title.setText("තත්‍ය කාලීන වංචා පරීක්ෂාව");
            if (step3Desc != null) step3Desc.setText("අපගේ පද්ධතිය මුදල, උපාංගය, ස්ථානය සහ හැසිරීම් රටා පරීක්ෂා කරයි.");
            if (step4Title != null) step4Title.setText("සාර්ථකයි");
            if (step4Desc != null) step4Desc.setText("මුදල් ක්ෂණිකව ලැබෙන අතර දෙපාර්ශවයටම දැනුම්දීම් ලැබේ.");
            if (previewTransferTitle != null) previewTransferTitle.setText("ක්ෂණික හුවමාරුව");
            if (previewProtectedBadge != null) previewProtectedBadge.setText("● ආරක්ෂිතයි");
            if (previewFromLabel != null) previewFromLabel.setText("යවන්නේ");
            if (previewToLabel != null) previewToLabel.setText("ලබන්නාගේ ගිණුම");
            if (previewAmountLabel != null) previewAmountLabel.setText("මුදල (USD)");
            if (sendMoneyBtn != null) sendMoneyBtn.setText("මුදල් යවන්න");
            if (previewVerdictLabel != null) previewVerdictLabel.setText("අවදානම් ලකුණු: අඩුයි · $250.00 යවන ලදී");

            // Security
            if (securityTitle != null) securityTitle.setText("වංචාව සිදුවීමට පෙර නවත්වන්න");
            if (securitySubtitle != null) securitySubtitle.setText("FraudShield සෑම ගනුදෙනුවක්ම තත්‍ය කාලීනව නිරීක්ෂණය කර මිලි තත්පර කිහිපයකින් සැකකටයුතු ක්‍රියා අවහිර කරයි.");
            if (sec1Title != null) sec1Title.setText("තත්‍ය කාලීන අවදානම් ලකුණු");
            if (sec1Desc != null) sec1Desc.setText("අනුමත කිරීමට පෙර සෑම ගනුදෙනුවකටම අවදානම් ලකුණක් ලැබේ.");
            if (sec2Title != null) sec2Title.setText("සම්පූර්ණ අන්තර්ගත සංකේතනය");
            if (sec2Desc != null) sec2Desc.setText("සියලුම සන්නිවේදනයන් TLS-ආරක්ෂිත සබඳතා හරහා සිදු වේ.");
            if (sec3Title != null) sec3Title.setText("ද්විත්ව සාධක ආරක්ෂණය (2FA)");
            if (sec3Desc != null) sec3Desc.setText("Login වීමේදී තනි-වර කේත (OTP) සහ විශ්වාසදායී උපාංග පරීක්ෂාව.");
            if (sec4Title != null) sec4Title.setText("ක්ෂණික දැනුම්දීම්");
            if (sec4Desc != null) sec4Desc.setText("කිසියම් අසාමාන්‍ය දෙයක් සිදුවූ වහාම දැනුම්දීම් ලබා ගන්න.");
            if (sec5Title != null) sec5Title.setText("ස්ථාන පරීක්ෂාවන් (Location Checks)");
            if (sec5Desc != null) sec5Desc.setText("අපේක්ෂිත නොවන ස්ථානවලින් සිදුවන login සහ ගෙවීම් හඳුනාගැනේ.");
            if (sec6Title != null) sec6Title.setText("හැසිරීම් රටා විශ්ලේෂණය");
            if (sec6Desc != null) sec6Desc.setText("ඔබගේ සාමාන්‍ය වියදම් රටාව හඳුනාගෙන අසාමාන්‍ය දේ වෙන්කර ගනී.");
            if (monitorTitle != null) monitorTitle.setText("සජීවී වංචා නිරීක්ෂකය");
            if (monitorLiveBadge != null) monitorLiveBadge.setText("සජීවී");
            if (monitorStat1Label != null) monitorStat1Label.setText("අනුමතයි");
            if (monitorStat2Label != null) monitorStat2Label.setText("සමාලෝචනය");
            if (monitorStat3Label != null) monitorStat3Label.setText("අවහිරයි");

            // Footer
            if (footerDesc != null) footerDesc.setText("තත්‍ය කාලීන වංචා හඳුනාගැනීම සමඟ සුරක්ෂිත බැංකු සේවාව. සෑම ගනුදෙනුවක්ම යාමට පෙර පරීක්ෂා කෙරේ.");
            if (footerBankingHeader != null) footerBankingHeader.setText("බැංකු සේවා");
            if (footerCompanyHeader != null) footerCompanyHeader.setText("සමාගම");
            if (footerSupportHeader != null) footerSupportHeader.setText("සහාය");
            if (footerStayUpdatedHeader != null) footerStayUpdatedHeader.setText("නවතම තොරතුරු");
            if (footerNewsletterSub != null) footerNewsletterSub.setText("ආරක්ෂක උපදෙස් සහ නවතම තොරතුරු, මසකට වරක්.");
            if (subscribeBtn != null) subscribeBtn.setText("දායක වන්න");
            if (footerAboutUs != null) footerAboutUs.setText("අප ගැන");
            if (footerOurTeam != null) footerOurTeam.setText("අපගේ කණ්ඩායම");
            if (footerCareers != null) footerCareers.setText("රැකියා අවස්ථා");
            if (footerBlog != null) footerBlog.setText("බ්ලොග් අඩවිය");
            if (footerHelpCenter != null) footerHelpCenter.setText("උපකාරක මධ්‍යස්ථානය");
            if (footerReportFraud != null) footerReportFraud.setText("වංචා වාර්තා කරන්න");
            if (footerFaqs != null) footerFaqs.setText("නිතර අසන ප්‍රශ්න");
            if (footerContactUs != null) footerContactUs.setText("අප අමතන්න");

        } else {
            // English (Original)
            if (languageMenuBtn != null) languageMenuBtn.setText("🌐 Eng");
            if (navAccounts != null) navAccounts.setText("Accounts");
            if (navCards != null) navCards.setText("Cards");
            if (navTransfers != null) navTransfers.setText("Transfers");
            if (navSecurity != null) navSecurity.setText("Security");
            if (signUpBtn != null) signUpBtn.setText("Sign Up");

            // Hero
            if (heroTitleLine1 != null) heroTitleLine1.setText("Easy Banking in");
            if (heroTitleLine2 != null) heroTitleLine2.setText("your busy life.");
            if (heroSubtitle != null) heroSubtitle.setText("Secure accounts, instant transfers and real-time\nfraud detection — all in one app.");
            if (getStartedBtn != null) getStartedBtn.setText("Sign In");

            // Phone mockup
            if (phoneWelcomeLabel != null) phoneWelcomeLabel.setText("Welcome back");
            if (phoneTotalBalanceLabel != null) phoneTotalBalanceLabel.setText("Total Balance");
            if (phoneSendLabel != null) phoneSendLabel.setText("Send");
            if (phoneRequestLabel != null) phoneRequestLabel.setText("Request");
            if (phoneShieldLabel != null) phoneShieldLabel.setText("Shield");
            if (phoneMoreLabel != null) phoneMoreLabel.setText("More");
            if (phoneRecentActivityLabel != null) phoneRecentActivityLabel.setText("Recent Activity");
            if (phoneShoppingLabel != null) phoneShoppingLabel.setText("Shopping");
            if (phoneBillsPaidLabel != null) phoneBillsPaidLabel.setText("Bills Paid");
            if (phoneTravelLabel != null) phoneTravelLabel.setText("Travel Booked");
            if (fraudToastTitle != null) fraudToastTitle.setText("Fraud blocked");
            if (fraudToastSub != null) fraudToastSub.setText("Suspicious payment\nstopped in real time");

            // Accounts
            if (accountsTitle != null) accountsTitle.setText("An account for every goal");
            if (accountsSubtitle != null) accountsSubtitle.setText("Open an account in minutes. Every account is protected by FraudShield's real-time monitoring from day one.");
            if (accountCard1Title != null) accountCard1Title.setText("Current Account");
            if (accountCard1Desc != null) accountCard1Desc.setText("For your everyday spending, salary and bill payments.");
            if (accountCard1Fee != null) accountCard1Fee.setText("$0 / monthly fee");
            if (accountCard1B1 != null) accountCard1B1.setText("✓  Free debit card");
            if (accountCard1B2 != null) accountCard1B2.setText("✓  Unlimited local transfers");
            if (accountCard1B3 != null) accountCard1B3.setText("✓  Instant spending alerts");
            if (openAccountBtn != null) openAccountBtn.setText("Open account");

            if (accountCard2Badge != null) accountCard2Badge.setText("Popular");
            if (accountCard2Title != null) accountCard2Title.setText("Savings Account");
            if (accountCard2Desc != null) accountCard2Desc.setText("Grow your money with competitive interest, paid monthly.");
            if (accountCard2Rate != null) accountCard2Rate.setText("6.5% p.a. interest");
            if (accountCard2B1 != null) accountCard2B1.setText("✓  Monthly interest payout");
            if (accountCard2B2 != null) accountCard2B2.setText("✓  Savings goals & round-ups");
            if (accountCard2B3 != null) accountCard2B3.setText("✓  No minimum balance");
            if (startSavingBtn != null) startSavingBtn.setText("Start saving");

            if (accountCard3Title != null) accountCard3Title.setText("Fixed Deposit");
            if (accountCard3Desc != null) accountCard3Desc.setText("Lock in a higher rate for a fixed period with guaranteed returns.");
            if (accountCard3Rate != null) accountCard3Rate.setText("9.0% p.a. up to 24 months");
            if (accountCard3B1 != null) accountCard3B1.setText("✓  Terms from 3 to 24 months");
            if (accountCard3B2 != null) accountCard3B2.setText("✓  Guaranteed returns");
            if (accountCard3B3 != null) accountCard3B3.setText("✓  Auto-renew option");
            if (openDepositBtn != null) openDepositBtn.setText("Open deposit");

            // Cards
            if (cardsTitle != null) cardsTitle.setText("Your card, fully under\nyour control");
            if (cardsSubtitle != null) cardsSubtitle.setText("Physical and virtual debit cards with controls you can change in a tap. Try them below.");
            if (cardFreezeTitle != null) cardFreezeTitle.setText("Freeze card");
            if (cardFreezeDesc != null) cardFreezeDesc.setText("Lost it? Freeze it instantly, unfreeze anytime.");
            if (cardOnlineTitle != null) cardOnlineTitle.setText("Online payments");
            if (cardOnlineDesc != null) cardOnlineDesc.setText("Allow or block online & overseas purchases.");
            if (cardLimitTitle != null) cardLimitTitle.setText("Daily spending limit");
            if (spendingSlider != null && spendingLimitLabel != null) {
                spendingLimitLabel.setText(String.format("Current limit: $%,.0f", spendingSlider.getValue()));
            }

            // Transfers
            if (transfersBadge != null) transfersBadge.setText("TRANSFERS");
            if (transfersTitle != null) transfersTitle.setText("Send money instantly —\nand safely");
            if (transfersSubtitle != null) transfersSubtitle.setText("Every transfer is scored for fraud risk before it leaves your account. Safe payments go through in seconds.");
            if (step1Title != null) step1Title.setText("Choose a recipient");
            if (step1Desc != null) step1Desc.setText("Pick a saved contact or enter an account number.");
            if (step2Title != null) step2Title.setText("Enter the amount");
            if (step2Desc != null) step2Desc.setText("Add a note so you both remember what it was for.");
            if (step3Title != null) step3Title.setText("Real-time fraud check");
            if (step3Desc != null) step3Desc.setText("Our engine checks amount, device, location and behaviour patterns.");
            if (step4Title != null) step4Title.setText("Done");
            if (step4Desc != null) step4Desc.setText("Money arrives instantly and both sides get a notification.");
            if (previewTransferTitle != null) previewTransferTitle.setText("Quick transfer");
            if (previewProtectedBadge != null) previewProtectedBadge.setText("● Protected");
            if (previewFromLabel != null) previewFromLabel.setText("From");
            if (previewToLabel != null) previewToLabel.setText("To account");
            if (previewAmountLabel != null) previewAmountLabel.setText("Amount (USD)");
            if (sendMoneyBtn != null) sendMoneyBtn.setText("Send money");
            if (previewVerdictLabel != null) previewVerdictLabel.setText("Risk score: low  ·  $250.00 sent to 1002 3345 6789");

            // Security
            if (securityTitle != null) securityTitle.setText("Fraud stopped before it happens");
            if (securitySubtitle != null) securitySubtitle.setText("FraudShield watches every transaction in real time and blocks suspicious activity in milliseconds.");
            if (sec1Title != null) sec1Title.setText("Real-time risk scoring");
            if (sec1Desc != null) sec1Desc.setText("Each transaction gets a risk score before approval.");
            if (sec2Title != null) sec2Title.setText("End-to-end encryption");
            if (sec2Desc != null) sec2Desc.setText("All client-server traffic travels over TLS-secured sockets.");
            if (sec3Title != null) sec3Title.setText("Two-factor login");
            if (sec3Desc != null) sec3Desc.setText("One-time codes and trusted-device checks on sign in.");
            if (sec4Title != null) sec4Title.setText("Instant alerts");
            if (sec4Desc != null) sec4Desc.setText("Get notified the moment something looks unusual.");
            if (sec5Title != null) sec5Title.setText("Location checks");
            if (sec5Desc != null) sec5Desc.setText("Flags logins and payments from unexpected places.");
            if (sec6Title != null) sec6Title.setText("Behaviour analysis");
            if (sec6Desc != null) sec6Desc.setText("Learns your normal spending to catch what isn't.");
            if (monitorTitle != null) monitorTitle.setText("Live Fraud monitor");
            if (monitorLiveBadge != null) monitorLiveBadge.setText("Live");
            if (monitorStat1Label != null) monitorStat1Label.setText("Approved");
            if (monitorStat2Label != null) monitorStat2Label.setText("Reviewed");
            if (monitorStat3Label != null) monitorStat3Label.setText("Blocked");

            // Footer
            if (footerDesc != null) footerDesc.setText("Secure banking with real-time fraud detection. Every transaction checked before it goes through.");
            if (footerBankingHeader != null) footerBankingHeader.setText("Banking");
            if (footerCompanyHeader != null) footerCompanyHeader.setText("Company");
            if (footerSupportHeader != null) footerSupportHeader.setText("Support");
            if (footerStayUpdatedHeader != null) footerStayUpdatedHeader.setText("Stay updated");
            if (footerNewsletterSub != null) footerNewsletterSub.setText("Security tips and product news, once a month.");
            if (subscribeBtn != null) subscribeBtn.setText("Subscribe");
            if (footerAboutUs != null) footerAboutUs.setText("About us");
            if (footerOurTeam != null) footerOurTeam.setText("Our team");
            if (footerCareers != null) footerCareers.setText("Careers");
            if (footerBlog != null) footerBlog.setText("Blog");
            if (footerHelpCenter != null) footerHelpCenter.setText("Help center");
            if (footerReportFraud != null) footerReportFraud.setText("Report fraud");
            if (footerFaqs != null) footerFaqs.setText("FAQs");
            if (footerContactUs != null) footerContactUs.setText("Contact us");
        }
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
                Scene scene = stage.getScene();
                if (scene != null) {
                    scene.setRoot(root);
                } else {
                    stage.setScene(new Scene(root));
                }
                stage.setTitle("FraudShield — Sign In");
                stage.setMaximized(true);
                stage.show();
                Platform.runLater(() -> stage.setMaximized(true));
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
                Scene scene = stage.getScene();
                if (scene != null) {
                    scene.setRoot(root);
                } else {
                    stage.setScene(new Scene(root));
                }
                stage.setTitle("FraudShield — Create a Secure Banking Account");
                stage.setMaximized(true);
                stage.show();
                Platform.runLater(() -> stage.setMaximized(true));
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
        return null;
    }
}
