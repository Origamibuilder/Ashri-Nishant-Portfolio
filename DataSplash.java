// package SE17;   // (Student) 

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;          // (AI-ADDED)
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;               // (AI-ADDED)
import java.util.List;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.table.*;

/**
 * DataSplash - Swim Time Predictor
 *
 * PROBLEM STATEMENT (Student):
 * ----------------------------
 * Competitive swimmers and coaches want to predict how a swimmer will do
 * in future races based on past meet results. It is hard to see trends
 * just by reading meet sheets or raw times online. This project builds
 * a Java GUI that:
 *   - Loads swim race data (CSV "database").
 *   - Lets you pick a swimmer and events.
 *   - Uses a simple trend line and comparison to other swimmers to
 *     predict a future time.
 *
 * PROJECT GOALS (Student):
 * ------------------------
 * - Use Java data structures (ArrayList, HashMap, etc).
 * - Do file I/O for reading CSV race data.
 * - Use basic statistics (linear regression) for the prediction.
 * - Practice event-driven programming and GUI design with Swing.
 *
 * AI DISCLOSURE:
 * --------------
 * Any code marked with "// (AI-ADDED)" was created or heavily edited
 * with help from an AI assistant. Everything else is my own work.
 */

public class DataSplash extends JFrame {

    // ======== MODEL CLASSES ========

    // (Student) Basic swimmer info so I can use it as a map key.
    static class Swimmer {
        String id;
        String name;
        String sex;
        int birthYear;

        // (Student) Constructor for Swimmer object
        Swimmer(String id, String name, int birthYear, String sex) {
            this.id = id;
            this.name = name;
            this.birthYear = birthYear;
            this.sex = sex;
        }

        // (Student) String representation for display
        @Override
        public String toString() {
            return name + " (" + sex + ", " + birthYear + ")";
        }

        // (Student) Equality check based on all fields
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Swimmer)) return false;
            Swimmer s = (Swimmer) o;
            return birthYear == s.birthYear &&
                    Objects.equals(id, s.id) &&
                    Objects.equals(name, s.name) &&
                    Objects.equals(sex, s.sex);
        }

        // (Student) Hash code for HashMap usage
        @Override
        public int hashCode() {
            return Objects.hash(id, name, sex, birthYear);
        }
    }

    // (Student) Represents a swim event like "100 Free SCY".
    static class Event {
        int distance;
        String stroke;
        String course;

        // (Student) Constructor for Event object
        Event(int distance, String stroke, String course) {
            this.distance = distance;
            this.stroke = stroke;
            this.course = course;
        }

        // (Student) String representation for display
        @Override
        public String toString() {
            return distance + " " + stroke + " (" + course + ")";
        }

        // (Student) Equality check based on all fields
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Event)) return false;
            Event e = (Event) o;
            return distance == e.distance &&
                    Objects.equals(stroke, e.stroke) &&
                    Objects.equals(course, e.course);
        }

        // (Student) Hash code for HashMap usage
        @Override
        public int hashCode() {
            return Objects.hash(distance, stroke, course);
        }
    }

    // (Student) One race result: swimmer, event, date, and time.
    static class Result {
        Swimmer swimmer;
        Event event;
        LocalDate date;
        double timeSec;

        // (Student) Constructor for Result object
        Result(Swimmer swimmer, Event event, LocalDate date, double timeSec) {
            this.swimmer = swimmer;
            this.event = event;
            this.date = date;
            this.timeSec = timeSec;
        }
    }

    // (Student) Might use this later to rank similar swimmers.
    static class SimilarityEntry {
        Swimmer other;
        double similarityScore;

        // (Student) Constructor for similarity comparison
        SimilarityEntry(Swimmer other, double similarityScore) {
            this.other = other;
            this.similarityScore = similarityScore;
        }
    }

    // ======== DATA STRUCTURES ========

    // (Student) This list holds all race results in memory.
    private final List<Result> allResults = new ArrayList<>();

    // (Student) Look up all races for a specific swimmer.
    private final Map<Swimmer, List<Result>> resultsBySwimmer = new HashMap<>();

    // (Student) Look up all races for a specific event.
    private final Map<Event, List<Result>> resultsByEvent = new HashMap<>();

    // ======== CORE UI WIDGETS (Student) ========

    // (Student) These fields are now used as "filters" or hints for DB filenames.
    private final JTextField tfTeam = new JTextField(20);
    private final JComboBox<String> cbGender =
            new JComboBox<>(new String[]{"M", "F"});
    private final JTextField tfYear = new JTextField("2024");

    private final JButton btnLoadDatabase =
            new JButton("📁 Load Swim Database"); // (AI-ADDED)
    private final JButton btnLoadCsv =
            new JButton("📄 Load Single CSV");         // (Student idea, label tweaked AI)

    private final JTextField tfSwimmerSearch = new JTextField(15);  // (Student)
    private final DefaultListModel<Swimmer> swimmersModel =
            new DefaultListModel<>();                                  // (Student)
    private final JList<Swimmer> lstSwimmers =
            new JList<>(swimmersModel);                                // (Student)

    private final DefaultListModel<Event> anchorEventsModel =
            new DefaultListModel<>();                                  // (Student)
    private final DefaultListModel<Event> targetEventsModel =
            new DefaultListModel<>();                                  // (Student)
    private final JList<Event> lstAnchorEvents =
            new JList<>(anchorEventsModel);                            // (Student)
    private final JList<Event> lstTargetEvents =
            new JList<>(targetEventsModel);                            // (Student)

    private final JSlider sliderEffort = new JSlider(1, 5, 3);        // (Student)
    private final JSlider sliderConsistency = new JSlider(1, 5, 3);   // (Student)

    private final JTextArea outputArea = new JTextArea();             // (Student)
    private final JLabel statusLabel = new JLabel("Ready.");          // (Student)

    private final JButton btnPredict =
            new JButton("🚀 Predict Time");                              // (Student)

    // (Student) Keep track of which swimmer is currently selected.
    private Swimmer selectedSwimmer = null;

    // ======== ENHANCED GUI STATE (AI-ADDED) ========

    // (AI-ADDED) Flag to flip between dark and light themes.
    private boolean darkTheme = true;

    // (AI-ADDED) Button in the status bar to toggle theme.
    private final JButton btnToggleTheme = new JButton("🌙 Switch to Light Theme");

    // (AI-ADDED) Progress bar so the UI feels responsive.
    private final JProgressBar progressBar = new JProgressBar();

    // (AI-ADDED) References I recolor on theme changes.
    private JPanel topPanel;
    private JPanel swimmersPanel;
    private JPanel eventsTrainingPanel;
    private JPanel outputPanel;
    private JPanel bottomPanel;

    // (AI-ADDED) Chart panel to visualize times and prediction.
    private PerformanceChartPanel chartPanel = new PerformanceChartPanel();  // (AI-ADDED)

    // ======== ENHANCED VISUAL COMPONENTS (AI-ADDED) ========
    private final JLabel lblTitle = new JLabel("DataSplash - Swim Time Predictor", JLabel.CENTER);
    private final JPanel headerPanel = new JPanel();
    private GradientPanel gradientPanel;
    private final JTable statsTable;
    private final DefaultTableModel statsTableModel;
    private final JLabel lblSwimmerCount = new JLabel("0", JLabel.CENTER);
    private final JLabel lblEventCount = new JLabel("0", JLabel.CENTER);
    private final JLabel lblResultCount = new JLabel("0", JLabel.CENTER);
    private int filesLoaded = 0; // (AI-ADDED) Track number of files loaded

    // ======== MANUAL ENTRY WIDGETS (AI-ADDED) ========

    private final JTextField tfManualSwimmerId   = new JTextField(8);   // (AI-ADDED)
    private final JTextField tfManualSwimmerName = new JTextField(12);  // (AI-ADDED)
    private final JComboBox<String> cbManualSex  =
            new JComboBox<>(new String[]{"M", "F"});                    // (AI-ADDED)
    private final JTextField tfManualBirthYear   = new JTextField(6);   // (AI-ADDED)
    private final JTextField tfManualDistance    = new JTextField(5);   // (AI-ADDED)
    private final JTextField tfManualStroke      = new JTextField(10);  // (AI-ADDED)
    private final JTextField tfManualCourse      = new JTextField(5);   // (AI-ADDED)
    private final JTextField tfManualDate        = new JTextField(10);  // (AI-ADDED)
    private final JTextField tfManualTime        = new JTextField(6);   // (AI-ADDED)
    private final JButton btnManualAdd           = new JButton("➕ Add Result"); // (AI-ADDED)

    // ======== CONSTRUCTOR ========

    // (Student) Builds the whole window and hooks up behavior.
    public DataSplash() {
        super("DataSplash – Swim Time Predictor");

        // (AI-ADDED) Set application icon for better window appearance
        try {
            setIconImage(createAppIcon());
        } catch (Exception e) {
            // Use default if custom icon fails
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 850); // (AI-ADDED) Slightly larger for better layout
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // (AI-ADDED) Create animated background panel
        gradientPanel = new GradientPanel();
        gradientPanel.setLayout(new BorderLayout());
        add(gradientPanel, BorderLayout.CENTER);

        // (AI-ADDED) Nice menu bar with File / Tools / Help.
        setJMenuBar(createMenuBar());

        // (AI-ADDED) Create enhanced header
        createHeader();

        // (AI-ADDED) Create stats table model for statistics tab
        statsTableModel = new DefaultTableModel(
            new Object[]{"Metric", "Value"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        statsTable = new JTable(statsTableModel);
        statsTable.setRowHeight(30);
        statsTable.setShowVerticalLines(false);
        statsTable.setIntercellSpacing(new Dimension(0, 5));

        topPanel = buildTopPanel();                  // (AI-ADDED layout)
        JSplitPane centerSplit = buildCenterPanel(); // (AI-ADDED layout)
        bottomPanel = buildBottomPanel();            // (AI-ADDED layout)

        gradientPanel.add(topPanel, BorderLayout.NORTH);
        gradientPanel.add(centerSplit, BorderLayout.CENTER);
        gradientPanel.add(bottomPanel, BorderLayout.SOUTH);

        // (Student) When I click a swimmer, update event lists.
        lstSwimmers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstSwimmers.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSwimmerSelected(lstSwimmers.getSelectedValue());
            }
        });

        // (Student) Filter swimmers by name as I type.
        tfSwimmerSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterSwimmersByName(tfSwimmerSearch.getText().trim().toLowerCase());
            }
        });

        // (Student + AI) Hook up main buttons.
        btnLoadCsv.addActionListener(e -> onLoadCsv());                  // (Student)
        btnLoadDatabase.addActionListener(e -> onLoadFromDatabase());    // (AI-ADDED)
        btnPredict.addActionListener(e -> onPredict());                  // (Student)
        btnManualAdd.addActionListener(e -> onAddManualResult());        // (AI-ADDED)

        // (AI-ADDED) Enhanced sliders with custom renderers
        setupEnhancedSliders();

        // (AI-ADDED) Theme switch hook.
        btnToggleTheme.addActionListener(e -> {
            darkTheme = !darkTheme;
            applyTheme();
        });

        applyTheme(); // (AI-ADDED) start with dark theme
    }

    // ======== ENHANCED UI COMPONENTS (AI-ADDED) ========

    // (AI-ADDED) Creates the header panel with title and quick stats
    private void createHeader() {
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setOpaque(false);
        
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(100, 180, 255));
        
        // (AI-ADDED) Add subtitle for better description
        JLabel lblSubtitle = new JLabel("AI-Powered Performance Prediction Platform", JLabel.CENTER);
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitle.setForeground(new Color(180, 220, 255));
        
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.add(lblTitle, BorderLayout.CENTER);
        titlePanel.add(lblSubtitle, BorderLayout.SOUTH);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        headerPanel.add(titlePanel, BorderLayout.CENTER);
        
        // (AI-ADDED) Add quick stats panel for immediate data overview
        JPanel statsPanel = createQuickStatsPanel();
        headerPanel.add(statsPanel, BorderLayout.EAST);
        
        gradientPanel.add(headerPanel, BorderLayout.NORTH);
    }

    // (AI-ADDED) Creates quick statistics panel showing dataset metrics
    private JPanel createQuickStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 180, 255, 100), 2),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        
        Font labelFont = new Font("Segoe UI", Font.BOLD, 11);
        Font valueFont = new Font("Segoe UI", Font.BOLD, 16);
        
        JLabel lblSwimmers = new JLabel("Swimmers:");
        lblSwimmers.setFont(labelFont);
        lblSwimmers.setForeground(new Color(200, 220, 255));
        
        JLabel lblEvents = new JLabel("Events:");
        lblEvents.setFont(labelFont);
        lblEvents.setForeground(new Color(200, 220, 255));
        
        JLabel lblResults = new JLabel("Results:");
        lblResults.setFont(labelFont);
        lblResults.setForeground(new Color(200, 220, 255));
        
        lblSwimmerCount.setFont(valueFont);
        lblSwimmerCount.setForeground(new Color(100, 255, 150));
        
        lblEventCount.setFont(valueFont);
        lblEventCount.setForeground(new Color(100, 255, 150));
        
        lblResultCount.setFont(valueFont);
        lblResultCount.setForeground(new Color(100, 255, 150));
        
        panel.add(lblSwimmers);
        panel.add(lblSwimmerCount);
        panel.add(lblEvents);
        panel.add(lblEventCount);
        panel.add(lblResults);
        panel.add(lblResultCount);
        
        return panel;
    }

    // (AI-ADDED) Sets up enhanced sliders with custom UI
    private void setupEnhancedSliders() {
        // (AI-ADDED) Custom slider UI for better appearance
        sliderEffort.setUI(new EnhancedSliderUI(sliderEffort));
        sliderConsistency.setUI(new EnhancedSliderUI(sliderConsistency));
        
        sliderEffort.setMajorTickSpacing(1);
        sliderEffort.setPaintTicks(true);
        sliderEffort.setPaintLabels(true);
        sliderEffort.setSnapToTicks(true);
        
        sliderConsistency.setMajorTickSpacing(1);
        sliderConsistency.setPaintTicks(true);
        sliderConsistency.setPaintLabels(true);
        sliderConsistency.setSnapToTicks(true);
        
        // (AI-ADDED) Tooltips for sliders to explain their purpose
        sliderEffort.setToolTipText("Training effort level (1=Low, 5=High)");
        sliderConsistency.setToolTipText("Training consistency (1=Low, 5=High)");
    }

    // ======== MENU / ABOUT / STATS (AI-ADDED) ========

    // (AI-ADDED) Builds the menu bar with file/tools/help.
    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("📁 File");
        fileMenu.setMnemonic('F');
        
        JMenuItem miLoadCsv = new JMenuItem("📄 Load Single CSV…");
        miLoadCsv.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        miLoadCsv.addActionListener(e -> onLoadCsv());
        
        JMenuItem miLoadDb = new JMenuItem("📁 Load from Swim Database…");
        miLoadDb.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK));
        miLoadDb.addActionListener(e -> onLoadFromDatabase());
        
        JMenuItem miExport = new JMenuItem("💾 Export Prediction Report…");
        miExport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
        miExport.addActionListener(e -> exportPredictionReport());
        
        JMenuItem miExit = new JMenuItem("❌ Exit");
        miExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        miExit.addActionListener(e -> dispose());
        
        fileMenu.add(miLoadCsv);
        fileMenu.add(miLoadDb);
        fileMenu.addSeparator();
        fileMenu.add(miExport);
        fileMenu.addSeparator();
        fileMenu.add(miExit);

        JMenu toolsMenu = new JMenu("⚙️ Tools");
        toolsMenu.setMnemonic('T');
        
        JMenuItem miClear = new JMenuItem("🗑️ Clear Prediction Output");
        miClear.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        miClear.addActionListener(e -> {
            outputArea.setText("");
            chartPanel.setNoData();
        });
        
        JMenuItem miStats = new JMenuItem("📊 Show Dataset Stats");
        miStats.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        miStats.addActionListener(e -> showStatsDialog());
        
        JMenuItem miCompare = new JMenuItem("📈 Compare Swimmers");
        miCompare.addActionListener(e -> showComparisonDialog());
        
        toolsMenu.add(miClear);
        toolsMenu.add(miStats);
        toolsMenu.add(miCompare);

        JMenu helpMenu = new JMenu("❓ Help");
        helpMenu.setMnemonic('H');
        
        JMenuItem miAbout = new JMenuItem("ℹ️ About DataSplash");
        miAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        miAbout.addActionListener(e -> showAboutDialog());
        
        JMenuItem miGuide = new JMenuItem("📘 User Guide");
        miGuide.addActionListener(e -> showUserGuide());
        
        helpMenu.add(miAbout);
        helpMenu.add(miGuide);

        bar.add(fileMenu);
        bar.add(toolsMenu);
        bar.add(helpMenu);
        return bar;
    }

    // (AI-ADDED) Enhanced about dialog with project details
    private void showAboutDialog() {
        JPanel aboutPanel = new JPanel(new BorderLayout(10, 10));
        aboutPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel title = new JLabel("DataSplash - Swim Time Predictor", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(100, 180, 255));
        
        JTextArea info = new JTextArea();
        info.setEditable(false);
        info.setOpaque(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setText(
            "Version: 2.0\n" +
            "Author: Nikhil Ashri\n\n" +
            "Description:\n" +
            "DataSplash is an advanced swim time prediction platform that uses statistical " +
            "analysis and machine learning concepts to forecast swim performance. " +
            "The application processes historical race data to identify trends, " +
            "compare swimmers, and generate accurate predictions.\n\n" +
            "Features:\n" +
            "• Multi-file database support\n" +
            "• Real-time data visualization\n" +
            "• Performance trend analysis\n" +
            "• Training factor adjustments\n" +
            "• Export capabilities\n\n" +
            "© 2024 DataSplash Project"
        );
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        aboutPanel.add(title, BorderLayout.NORTH);
        aboutPanel.add(new JScrollPane(info), BorderLayout.CENTER);
        
        JOptionPane.showMessageDialog(
            this,
            aboutPanel,
            "About DataSplash",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    // (AI-ADDED) User guide dialog with instructions
    private void showUserGuide() {
        String guide = """
            1. LOADING DATA
               • Use 'Load Single CSV' for individual files
               • Use 'Load Database' for organized CSV collections
               • Format: swimmerId,name,birthYear,sex,distance,stroke,course,date,time
            
            2. SELECTING SWIMMER
               • Click any swimmer in the left panel
               • Use search to filter by name
            
            3. SETTING PREDICTION
               • Choose Anchor Event (known performance)
               • Choose Target Event (prediction target)
               • Adjust training sliders for effort/consistency
               • Click Predict Time
            
            4. INTERPRETING RESULTS
               • View text report in output tab
               • Check performance chart for trends
               • Compare with other swimmers
            
            5. TIPS
               • More historical data = better predictions
               • Keep training factors realistic
               • Use consistent date formats
            """;
        
        JTextArea textArea = new JTextArea(guide);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setOpaque(false);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        
        JOptionPane.showMessageDialog(
            this,
            scrollPane,
            "User Guide",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    // (AI-ADDED) Enhanced stats dialog with table visualization
    private void showStatsDialog() {
        int swimmerCount = resultsBySwimmer.size();
        int eventCount = resultsByEvent.size();
        int resultCount = allResults.size();

        Optional<LocalDate> earliest = allResults.stream()
                .map(r -> r.date)
                .min(LocalDate::compareTo);
        Optional<LocalDate> latest = allResults.stream()
                .map(r -> r.date)
                .max(LocalDate::compareTo);

        OptionalDouble bestTimeOpt = allResults.stream()
                .mapToDouble(r -> r.timeSec)
                .min();
        
        OptionalDouble avgTimeOpt = allResults.stream()
                .mapToDouble(r -> r.timeSec)
                .average();

        // (AI-ADDED) Calculate gender distribution
        long maleCount = allResults.stream()
                .filter(r -> r.swimmer.sex.equals("M"))
                .map(r -> r.swimmer)
                .distinct()
                .count();
        long femaleCount = allResults.stream()
                .filter(r -> r.swimmer.sex.equals("F"))
                .map(r -> r.swimmer)
                .distinct()
                .count();

        // (AI-ADDED) Prepare table data for display
        Object[][] data = {
            {"Total Swimmers", swimmerCount},
            {"Total Events", eventCount},
            {"Total Results", resultCount},
            {"Male Swimmers", maleCount},
            {"Female Swimmers", femaleCount},
            {"Date Range", earliest.map(LocalDate::toString).orElse("N/A") + " to " + 
                           latest.map(LocalDate::toString).orElse("N/A")},
            {"Fastest Time (s)", bestTimeOpt.isPresent() ? 
                String.format("%.2f", bestTimeOpt.getAsDouble()) : "N/A"},
            {"Average Time (s)", avgTimeOpt.isPresent() ? 
                String.format("%.2f", avgTimeOpt.getAsDouble()) : "N/A"},
            {"Data Density", resultCount > 0 ? 
                String.format("%.1f results/swimmer", (double)resultCount/swimmerCount) : "N/A"}
        };

        DefaultTableModel model = new DefaultTableModel(data, 
            new String[]{"Statistic", "Value"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(450, 300));
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Dataset Statistics", JLabel.CENTER), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(
            this,
            panel,
            "Dataset Statistics",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    // (AI-ADDED) Swimmer comparison dialog
    private void showComparisonDialog() {
        if (resultsBySwimmer.size() < 2) {
            showError("Need at least 2 swimmers for comparison.");
            return;
        }

        JDialog dialog = new JDialog(this, "Compare Swimmers", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JComboBox<Swimmer> cbSwimmer1 = new JComboBox<>();
        JComboBox<Swimmer> cbSwimmer2 = new JComboBox<>();
        JComboBox<Event> cbEvent = new JComboBox<>();

        for (Swimmer s : resultsBySwimmer.keySet()) {
            cbSwimmer1.addItem(s);
            cbSwimmer2.addItem(s);
        }

        // Populate events based on common events
        cbSwimmer1.addActionListener(e -> updateComparisonEvents(cbSwimmer1, cbSwimmer2, cbEvent));
        cbSwimmer2.addActionListener(e -> updateComparisonEvents(cbSwimmer1, cbSwimmer2, cbEvent));

        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        inputPanel.add(new JLabel("Swimmer 1:"));
        inputPanel.add(cbSwimmer1);
        inputPanel.add(new JLabel("Swimmer 2:"));
        inputPanel.add(cbSwimmer2);
        inputPanel.add(new JLabel("Event:"));
        inputPanel.add(cbEvent);
        
        JButton btnCompare = new JButton("Compare");
        btnCompare.addActionListener(e -> {
            Swimmer s1 = (Swimmer) cbSwimmer1.getSelectedItem();
            Swimmer s2 = (Swimmer) cbSwimmer2.getSelectedItem();
            Event ev = (Event) cbEvent.getSelectedItem();
            if (s1 != null && s2 != null && ev != null) {
                showComparisonResults(s1, s2, ev);
            }
        });
        
        inputPanel.add(new JLabel());
        inputPanel.add(btnCompare);

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        
        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        mainPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    // (AI-ADDED) Export prediction report to file
    private void exportPredictionReport() {
        if (outputArea.getText().trim().isEmpty()) {
            showError("No prediction results to export.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("prediction_report_" + 
            LocalDate.now().toString() + ".txt"));
        
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                Files.write(file.toPath(), outputArea.getText().getBytes());
                JOptionPane.showMessageDialog(this, 
                    "Report exported successfully to:\n" + file.getAbsolutePath(),
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                showError("Failed to export report: " + e.getMessage());
            }
        }
    }

    // ======== LAYOUT / THEMING (AI-ADDED) ========

    // (AI-ADDED) Top panel with team / gender / year and load buttons.
    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder("📊 Swim Data Source / Filters"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTeam = new JLabel("🏊 Team (for DB filename):");
        JLabel lblGender = new JLabel("⚥ Gender:");
        JLabel lblYear = new JLabel("📅 Year:");

        c.gridx = 0; c.gridy = 0;
        panel.add(lblTeam, c);
        c.gridx = 1; c.gridy = 0; c.weightx = 1.0;
        panel.add(tfTeam, c);
        c.weightx = 0.0;

        c.gridx = 0; c.gridy = 1;
        panel.add(lblGender, c);
        c.gridx = 1; c.gridy = 1;
        panel.add(cbGender, c);

        c.gridx = 0; c.gridy = 2;
        panel.add(lblYear, c);
        c.gridx = 1; c.gridy = 2;
        panel.add(tfYear, c);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnLoadDatabase);
        buttonPanel.add(btnLoadCsv);
        panel.add(buttonPanel, c);

        return panel;
    }

    // (AI-ADDED) Builds the manual entry panel.
    private JPanel buildManualEntryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder("✍️ Add Result Manually"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        int row = 0;

        // Row 0: Swimmer ID
        c.gridx = 0; c.gridy = row;
        panel.add(new JLabel("🆔 Swimmer ID:"), c);
        c.gridx = 1;
        panel.add(tfManualSwimmerId, c);
        row++;

        // Row 1: Name
        c.gridx = 0; c.gridy = row;
        panel.add(new JLabel("👤 Name:"), c);
        c.gridx = 1;
        panel.add(tfManualSwimmerName, c);
        row++;

        // Row 2: Sex
        c.gridx = 0; c.gridy = row;
        panel.add(new JLabel("⚥ Sex:"), c);
        c.gridx = 1;
        panel.add(cbManualSex, c);
        row++;

        // Row 3: Birth Year
        c.gridx = 0; c.gridy = row;
        panel.add(new JLabel("🎂 Birth Year:"), c);
        c.gridx = 1;
        panel.add(tfManualBirthYear, c);
        row++;

        // Row 4: Distance
        c.gridx = 0; c.gridy = row;
        panel.add(new JLabel("📏 Distance:"), c);
        c.gridx = 1;
        panel.add(tfManualDistance, c);
        row++;

        // Row 5: Stroke
        c.gridx = 0; c.gridy = row;
        panel.add(new JLabel("🏊 Stroke:"), c);
        c.gridx = 1;
        panel.add(tfManualStroke, c);
        row++;

        // Row 6: Course
        c.gridx = 0; c.gridy = row;
        panel.add(new JLabel("Course:"), c);
        c.gridx = 1;
        panel.add(tfManualCourse, c);
        row++;

        // Row 7: Date
        c.gridx = 0; c.gridy = row;
        panel.add(new JLabel("📅 Date (YYYY-MM-DD):"), c);
        c.gridx = 1;
        panel.add(tfManualDate, c);
        row++;

        // Row 8: Time
        c.gridx = 0; c.gridy = row;
        panel.add(new JLabel("⏱️ Time (seconds):"), c);
        c.gridx = 1;
        panel.add(tfManualTime, c);
        row++;

        // Row 9: Add button
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        panel.add(btnManualAdd, c);
        row++;

        // Row 10: Little hint label
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        JLabel hint = new JLabel("💡 Tip: Fill all fields, then click 'Add Result'.");
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        panel.add(hint, c);

        return panel;
    }

    // (AI-ADDED) Middle section with swimmers (left) and right multi-panel.
    private JSplitPane buildCenterPanel() {
        // Swimmers column
        swimmersPanel = new JPanel(new BorderLayout(4, 4));
        swimmersPanel.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder("👥 Swimmers"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JPanel searchPanel = new JPanel(new BorderLayout(4, 4));
        searchPanel.setOpaque(false);
        JLabel lblSearch = new JLabel("🔍 Search:");
        searchPanel.add(lblSearch, BorderLayout.WEST);
        searchPanel.add(tfSwimmerSearch, BorderLayout.CENTER);

        swimmersPanel.add(searchPanel, BorderLayout.NORTH);
        
        // (AI-ADDED) Enhanced swimmer list with custom renderer
        lstSwimmers.setCellRenderer(new SwimmerListRenderer());
        swimmersPanel.add(new JScrollPane(lstSwimmers), BorderLayout.CENTER);

        // Right side split: top = events/training/manual, bottom = output.
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplit.setResizeWeight(0.55);
        rightSplit.setBorder(null);

        eventsTrainingPanel = new JPanel(new GridLayout(1, 4, 10, 6));
        eventsTrainingPanel.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder("🎯 Events, Training & Manual Entry"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        //Train data to fit within panel 
        JPanel anchorPanel = new JPanel(new BorderLayout());
        anchorPanel.setBorder(new TitledBorder("⚓ Anchor Event"));
        lstAnchorEvents.setCellRenderer(new EventListRenderer());
        anchorPanel.add(new JScrollPane(lstAnchorEvents), BorderLayout.CENTER);

        JPanel targetPanel = new JPanel(new BorderLayout());
        targetPanel.setBorder(new TitledBorder("🎯 Target Event"));
        lstTargetEvents.setCellRenderer(new EventListRenderer());
        targetPanel.add(new JScrollPane(lstTargetEvents), BorderLayout.CENTER);

        JPanel trainingPanel = new JPanel();
        trainingPanel.setBorder(new TitledBorder("🏋️ Training (1 = low, 5 = high)"));
        trainingPanel.setLayout(new BoxLayout(trainingPanel, BoxLayout.Y_AXIS));

        JLabel lblEffort = new JLabel("💪 Effort:");
        JLabel lblConsistency = new JLabel("📈 Consistency:");

        //Create vertical lines
        trainingPanel.add(Box.createVerticalStrut(5));
        trainingPanel.add(lblEffort);
        trainingPanel.add(sliderEffort);
        trainingPanel.add(Box.createVerticalStrut(15));
        trainingPanel.add(lblConsistency);
        trainingPanel.add(sliderConsistency);
        trainingPanel.add(Box.createVerticalStrut(20));

        btnPredict.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPredict.setMaximumSize(new Dimension(200, 40));
        btnPredict.setFont(new Font("Segoe UI", Font.BOLD, 14));
        trainingPanel.add(btnPredict);

        JPanel manualPanel = buildManualEntryPanel();

        eventsTrainingPanel.add(anchorPanel);
        eventsTrainingPanel.add(targetPanel);
        eventsTrainingPanel.add(trainingPanel);
        eventsTrainingPanel.add(manualPanel);

        rightSplit.setTopComponent(eventsTrainingPanel);

        outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(new TitledBorder("📊 Prediction Details"));
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // (AI-ADDED) Enhanced tabbed output
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JScrollPane textScroll = new JScrollPane(outputArea);
        textScroll.setBorder(BorderFactory.createEmptyBorder());
        tabs.addTab("📝 Text Report", createTabIcon("📝"), textScroll);
        tabs.addTab("📈 Performance Chart", createTabIcon("📈"), chartPanel);
        
        // (AI-ADDED) Add statistics tab
        JPanel statsTab = createStatisticsTab();
        tabs.addTab("📊 Statistics", createTabIcon("📊"), statsTab);
        
        outputPanel.add(tabs, BorderLayout.CENTER);

        rightSplit.setBottomComponent(outputPanel);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, swimmersPanel, rightSplit);
        split.setResizeWeight(0.25);
        split.setDividerLocation(300);
        split.setBorder(null);

        return split;
    }

    // (AI-ADDED) Create icon for tabs
    private ImageIcon createTabIcon(String emoji) {
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        FontMetrics fm = g2.getFontMetrics();
        int x = (16 - fm.stringWidth(emoji)) / 2;
        int y = (16 + fm.getAscent()) / 2 - 2;
        g2.drawString(emoji, x, y);
        g2.dispose();
        return new ImageIcon(icon);
    }

    // (AI-ADDED) Create statistics tab panel
    private JPanel createStatisticsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Update stats table with current data
        updateStatisticsTable();
        
        JScrollPane scrollPane = new JScrollPane(statsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRefreshStats = new JButton("🔄 Refresh Statistics");
        btnRefreshStats.addActionListener(e -> updateStatisticsTable());
        buttonPanel.add(btnRefreshStats);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    // (AI-ADDED) Update statistics table with current data
    private void updateStatisticsTable() {
        statsTableModel.setRowCount(0);
        
        if (allResults.isEmpty()) {
            statsTableModel.addRow(new Object[]{"No data loaded", ""});
            return;
        }
        
        int swimmerCount = resultsBySwimmer.size();
        int eventCount = resultsByEvent.size();
        int resultCount = allResults.size();
        
        Optional<LocalDate> earliest = allResults.stream()
                .map(r -> r.date)
                .min(LocalDate::compareTo);
        Optional<LocalDate> latest = allResults.stream()
                .map(r -> r.date)
                .max(LocalDate::compareTo);
        
        OptionalDouble avgTime = allResults.stream()
                .mapToDouble(r -> r.timeSec)
                .average();
        
        OptionalDouble bestTime = allResults.stream()
                .mapToDouble(r -> r.timeSec)
                .min();
        
        long maleCount = allResults.stream()
                .filter(r -> r.swimmer.sex.equals("M"))
                .map(r -> r.swimmer)
                .distinct()
                .count();
        
        long femaleCount = allResults.stream()
                .filter(r -> r.swimmer.sex.equals("F"))
                .map(r -> r.swimmer)
                .distinct()
                .count();
        
        // Calculate average results per swimmer
        double avgResultsPerSwimmer = swimmerCount > 0 ? 
                (double) resultCount / swimmerCount : 0;
        
        statsTableModel.addRow(new Object[]{"Total Swimmers", swimmerCount});
        statsTableModel.addRow(new Object[]{"Male Swimmers", maleCount});
        statsTableModel.addRow(new Object[]{"Female Swimmers", femaleCount});
        statsTableModel.addRow(new Object[]{"Total Events", eventCount});
        statsTableModel.addRow(new Object[]{"Total Results", resultCount});
        statsTableModel.addRow(new Object[]{"Date Range", 
            earliest.map(d -> d.toString()).orElse("N/A") + " - " +
            latest.map(d -> d.toString()).orElse("N/A")});
        statsTableModel.addRow(new Object[]{"Average Time", 
            avgTime.isPresent() ? String.format("%.2f sec", avgTime.getAsDouble()) : "N/A"});
        statsTableModel.addRow(new Object[]{"Best Time", 
            bestTime.isPresent() ? String.format("%.2f sec", bestTime.getAsDouble()) : "N/A"});
        statsTableModel.addRow(new Object[]{"Avg Results/Swimmer", 
            String.format("%.1f", avgResultsPerSwimmer)});
    }

    // (AI-ADDED) Bottom bar with status text, progress bar, and theme toggle.
    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // Status label with icon
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusPanel.setOpaque(false);
        JLabel statusIcon = new JLabel("💬");
        statusPanel.add(statusIcon);
        statusPanel.add(statusLabel);
        
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(160, 20));
        progressBar.setStringPainted(true);
        
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(progressBar);
        right.add(btnToggleTheme);

        panel.add(statusPanel, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    // (AI-ADDED) Apply colors based on darkTheme flag.
    private void applyTheme() {
        Color bg, fg, listBg, listFg, panelBg, borderColor;

        if (darkTheme) {
            bg = new Color(18, 18, 30);
            fg = new Color(245, 245, 245);
            listBg = new Color(26, 35, 126);
            listFg = fg;
            panelBg = new Color(30, 30, 45, 200);
            borderColor = new Color(100, 180, 255, 100);
            btnToggleTheme.setText("🌙 Switch to Light Theme");
        } else {
            bg = new Color(240, 248, 255);
            fg = Color.BLACK;
            listBg = Color.BLACK;
            listFg = Color.BLACK;
            panelBg = new Color(255, 255, 255, 200);
            borderColor = new Color(100, 180, 255);
            btnToggleTheme.setText("☀️ Switch to Dark Theme");
        }

        gradientPanel.setColors(darkTheme);
        getContentPane().setBackground(bg);
        
        // Apply theme to all panels
        Component[] components = {topPanel, swimmersPanel, eventsTrainingPanel, 
                                 outputPanel, bottomPanel, headerPanel};
        for (Component comp : components) {
            if (comp != null) {
                comp.setBackground(panelBg);
                if (comp instanceof JPanel) {
                    ((JPanel) comp).setOpaque(true);
                }
            }
        }

        // Apply borders with theme colors
        applyThemeBorders(borderColor);

        lstSwimmers.setBackground(listBg);
        lstSwimmers.setForeground(listFg);
        lstAnchorEvents.setBackground(listBg);
        lstAnchorEvents.setForeground(listFg);
        lstTargetEvents.setBackground(listBg);
        lstTargetEvents.setForeground(listFg);

        outputArea.setBackground(darkTheme ? new Color(10, 10, 20) : Color.WHITE);
        outputArea.setForeground(fg);
        outputArea.setCaretColor(fg);

        statusLabel.setForeground(fg);

        if (chartPanel != null) {
            chartPanel.setBackground(darkTheme ? new Color(20, 20, 35) : Color.WHITE);
            chartPanel.setForeground(fg);
        }

        // Update quick stats colors
        lblSwimmerCount.setForeground(darkTheme ? new Color(100, 255, 150) : new Color(0, 150, 0));
        lblEventCount.setForeground(darkTheme ? new Color(100, 255, 150) : new Color(0, 150, 0));
        lblResultCount.setForeground(darkTheme ? new Color(100, 255, 150) : new Color(0, 150, 0));

        SwingUtilities.updateComponentTreeUI(this);
    }

    // (AI-ADDED) Apply themed borders to panels
private void applyThemeBorders(Color borderColor) {
    Border roundedBorder = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(borderColor, 1),
        BorderFactory.createEmptyBorder(5, 5, 5, 5)
    );

    // Check each panel and safely update border colors
    JPanel[] panels = {topPanel, swimmersPanel, eventsTrainingPanel, outputPanel};
    
    for (JPanel panel : panels) {
        if (panel != null) {
            Border currentBorder = panel.getBorder();
            if (currentBorder != null) {
                if (currentBorder instanceof TitledBorder) {
                    // If it's just a TitledBorder, update its color
                    ((TitledBorder) currentBorder).setTitleColor(borderColor);
                } else if (currentBorder instanceof CompoundBorder) {
                    // If it's a CompoundBorder, check if the outside border is TitledBorder
                    Border outsideBorder = ((CompoundBorder) currentBorder).getOutsideBorder();
                    if (outsideBorder instanceof TitledBorder) {
                        ((TitledBorder) outsideBorder).setTitleColor(borderColor);
                    }
                }
            }
        }
    }
}

    // ======== ENHANCED RENDERERS (AI-ADDED) ========

    // (AI-ADDED) Custom renderer for swimmer list
    private class SwimmerListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Swimmer) {
                Swimmer s = (Swimmer) value;
                setText(s.name + " (" + s.sex + ", " + s.birthYear + ")");
                
                // Add icon based on gender
                if (s.sex.equals("M")) {
                    setIcon(createGenderIcon("M"));
                } else {
                    setIcon(createGenderIcon("F"));
                }
                
                // Enhanced selection appearance
                if (isSelected) {
                    setBackground(new Color(100, 180, 255, 100));
                    setForeground(Color.WHITE);
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(100, 180, 255), 2),
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)
                    ));
                }
            }
            return this;
        }
    }

    // (AI-ADDED) Create gender icon for swimmer list
    private ImageIcon createGenderIcon(String gender) {
        int size = 16;
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (gender.equals("M")) {
            // Male icon (blue)
            g2.setColor(new Color(100, 180, 255));
            g2.fillOval(0, 0, size, size);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.drawString("♂", 4, size-5);
        } else {
            // Female icon (pink)
            g2.setColor(new Color(255, 100, 180));
            g2.fillOval(0, 0, size, size);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.drawString("♀", 4, size-5);
        }
        
        g2.dispose();
        return new ImageIcon(icon);
    }

    // (AI-ADDED) Custom renderer for event list
    private static class EventListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Event) {
                Event e = (Event) value;
                
                // Color code by stroke
                Color strokeColor = getStrokeColor(e.stroke);
                setForeground(strokeColor);
                
                if (isSelected) {
                    setBackground(new Color(strokeColor.getRed(), strokeColor.getGreen(), 
                                           strokeColor.getBlue(), 50));
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(strokeColor, 1),
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)
                    ));
                }
            }
            return this;
        }
        
        // (AI-ADDED) Get color based on stroke type
        private Color getStrokeColor(String stroke) {
            switch (stroke.toLowerCase()) {
                case "free": return new Color(100, 180, 255);
                case "back": return new Color(100, 255, 180);
                case "breast": return new Color(255, 180, 100);
                case "fly": return new Color(255, 100, 180);
                case "im": return new Color(180, 100, 255);
                default: return Color.WHITE;
            }
        }
    }

       // (AI-ADDED) Custom slider UI for enhanced appearance
    private static class EnhancedSliderUI extends javax.swing.plaf.basic.BasicSliderUI {
        private static final int TRACK_HEIGHT = 6;
        private static final int THUMB_SIZE = 20;
        private final JSlider slider; // Store reference to slider
        
        public EnhancedSliderUI(JSlider slider) {
            super(slider);
            this.slider = slider;
        }
        
        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                               RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Get track bounds from parent class
            Rectangle trackBounds = this.trackRect;
            int cy = (trackBounds.height - TRACK_HEIGHT) / 2 + trackBounds.y;
            
            // Paint track background
            g2.setColor(new Color(60, 60, 80));
            g2.fillRoundRect(trackBounds.x, cy, trackBounds.width, TRACK_HEIGHT, 
                           TRACK_HEIGHT, TRACK_HEIGHT);
            
            // Paint filled track with gradient
            int fillWidth = this.thumbRect.x - trackBounds.x + THUMB_SIZE / 2;
            if (fillWidth > 0) {
                GradientPaint gradient = new GradientPaint(
                    trackBounds.x, cy, new Color(100, 180, 255),
                    trackBounds.x + fillWidth, cy, new Color(50, 120, 255)
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(trackBounds.x, cy, fillWidth, TRACK_HEIGHT, 
                               TRACK_HEIGHT, TRACK_HEIGHT);
            }
        }
        
        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                               RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Get thumb bounds from parent class
            Rectangle thumbBounds = this.thumbRect;
            int x = thumbBounds.x + thumbBounds.width / 2 - THUMB_SIZE / 2;
            int y = thumbBounds.y + thumbBounds.height / 2 - THUMB_SIZE / 2;
            
            // Paint thumb with shadow
            g2.setColor(new Color(30, 30, 40));
            g2.fillOval(x + 2, y + 2, THUMB_SIZE, THUMB_SIZE);
            
            // Paint thumb with gradient
            GradientPaint gradient = new GradientPaint(
                x, y, new Color(100, 200, 255),
                x + THUMB_SIZE, y + THUMB_SIZE, new Color(50, 150, 255)
            );
            g2.setPaint(gradient);
            g2.fillOval(x, y, THUMB_SIZE, THUMB_SIZE);
            
            // Paint thumb border
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(x, y, THUMB_SIZE, THUMB_SIZE);
            
            // Paint value on thumb
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            String value = String.valueOf(this.slider.getValue());
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(value);
            int textHeight = fm.getHeight();
            g2.drawString(value, x + (THUMB_SIZE - textWidth) / 2, 
                         y + (THUMB_SIZE + textHeight) / 2 - 2);
        }
        
        @Override
        protected Dimension getThumbSize() {
            return new Dimension(THUMB_SIZE, THUMB_SIZE);
        }
    }

    // (AI-ADDED) Gradient background panel with animation
    private class GradientPanel extends JPanel {
        private boolean darkMode = true;
        private float[] fractions = {0.0f, 0.5f, 1.0f};
        private Color[] darkColors = {
            new Color(18, 18, 30),
            new Color(30, 30, 50),
            new Color(18, 18, 30)
        };
        private Color[] lightColors = {
            new Color(240, 248, 255),
            new Color(220, 240, 255),
            new Color(240, 248, 255)
        };
        private int animationOffset = 0;
        private Timer animationTimer;
        
        public GradientPanel() {
            // Create animation timer for background movement
            animationTimer = new Timer(50, e -> {
                animationOffset = (animationOffset + 1) % 100;
                repaint();
            });
            animationTimer.start();
        }
        
        public void setColors(boolean darkMode) {
            this.darkMode = darkMode;
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            
            Color[] colors = darkMode ? darkColors : lightColors;
            
            // Create animated gradient
            Point2D start = new Point2D.Float(animationOffset, 0);
            Point2D end = new Point2D.Float(getWidth() + animationOffset, getHeight());
            
            LinearGradientPaint gradient = new LinearGradientPaint(
                start, end, fractions, colors
            );
            
            g2.setPaint(gradient);
            g2.fillRect(0, 0, getWidth(), getHeight());
            
            // Add subtle grid pattern
            g2.setColor(darkMode ? new Color(255, 255, 255, 10) : 
                                  new Color(0, 0, 0, 10));
            int gridSize = 40;
            for (int x = 0; x < getWidth(); x += gridSize) {
                g2.drawLine(x, 0, x, getHeight());
            }
            for (int y = 0; y < getHeight(); y += gridSize) {
                g2.drawLine(0, y, getWidth(), y);
            }
            
            g2.dispose();
        }
        
        // Clean up timer when panel is no longer needed
        @Override
        public void removeNotify() {
            super.removeNotify();
            if (animationTimer != null) {
                animationTimer.stop();
            }
        }
    }

    // ======== BUSY / ERROR HELPERS (AI-ADDED) ========

    // (AI-ADDED) Show/hide the spinner and disable buttons during long work.
    private void setBusy(boolean busy, String message) {
        btnLoadCsv.setEnabled(!busy);
        btnLoadDatabase.setEnabled(!busy);
        btnPredict.setEnabled(!busy);
        tfSwimmerSearch.setEnabled(!busy);
        btnManualAdd.setEnabled(!busy);

        progressBar.setVisible(busy);
        progressBar.setIndeterminate(busy);
        progressBar.setString(busy ? "Processing..." : "");
        
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getDefaultCursor());

        if (message != null) {
            statusLabel.setText(message);
        }
    }

    // (AI-ADDED) Enhanced error dialog with icon
    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this,
            createErrorPanel(message),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
    
    // (AI-ADDED) Create error panel for error messages
    private JPanel createErrorPanel(String message) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel iconLabel = new JLabel("❌");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        
        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setOpaque(false);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        
        return panel;
    }

    // ======== DATA LOADING (Student + AI helper) ========

    /**
     * (Student) Read a CSV file with these columns:
     * swimmerId, swimmerName, birthYear, sex,
     * distance, stroke, course,
     * date(YYYY-MM-DD), timeSeconds
     */
    private void loadCsv(Path path) throws IOException {
        allResults.clear();                                // (Student)
        loadCsvInto(allResults, path);                     // (AI-ADDED helper reuse)
        indexResults();                                    // (Student)
        rebuildSwimmerList();                              // (Student)
        updateEventListsForSwimmer();                      // (Student)
        updateQuickStats();                                // (AI-ADDED)
        updateStatisticsTable();                           // (AI-ADDED)
    }

    /**
     * (AI-ADDED) Update quick stats display with current counts
     */
    private void updateQuickStats() {
        lblSwimmerCount.setText(String.valueOf(resultsBySwimmer.size()));
        lblEventCount.setText(String.valueOf(resultsByEvent.size()));
        lblResultCount.setText(String.valueOf(allResults.size()));
    }

    /**
     * (AI-ADDED) Lower-level CSV loader that appends into a given list.
     * This lets me reuse parsing logic for single files and database folders.
     */
    private void loadCsvInto(List<Result> dest, Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        int lineNumber = 0;
        int successful = 0;
        
        for (String line : lines) {
            lineNumber++;
            if (line.trim().isEmpty() || line.startsWith("#")) continue;
            
            try {
                String[] t = line.split(",");
                if (t.length < 9) {
                    System.err.println("Warning: Line " + lineNumber + " has insufficient columns: " + line);
                    continue;
                }

                Swimmer s = new Swimmer(
                        t[0].trim(),
                        t[1].trim(),
                        Integer.parseInt(t[2].trim()),
                        t[3].trim()
                );
                Event e = new Event(
                        Integer.parseInt(t[4].trim()),
                        t[5].trim(),
                        t[6].trim()
                );
                LocalDate date = LocalDate.parse(t[7].trim());
                double time = Double.parseDouble(t[8].trim());

                Result r = new Result(s, e, date, time);
                dest.add(r);
                successful++;
                
            } catch (Exception e) {
                System.err.println("Error parsing line " + lineNumber + ": " + e.getMessage());
            }
        }
        
        System.out.println("Successfully loaded " + successful + " records from " + path.getFileName());
    }

    /**
     * (Student) Build the two main maps for fast lookups.
     */
    private void indexResults() {
        resultsBySwimmer.clear();
        resultsByEvent.clear();

        for (Result r : allResults) {
            resultsBySwimmer.computeIfAbsent(r.swimmer, k -> new ArrayList<>()).add(r);
            resultsByEvent.computeIfAbsent(r.event, k -> new ArrayList<>()).add(r);
        }
    }

    /**
     * (Student) Rebuild the swimmer list shown in the left list box.
     */
    private void rebuildSwimmerList() {
        swimmersModel.clear();
        List<Swimmer> swimmers = new ArrayList<>(resultsBySwimmer.keySet());
        swimmers.sort(Comparator.comparing(s -> s.name));
        for (Swimmer s : swimmers) {
            swimmersModel.addElement(s);
        }
    }

    /**
     * (Student) Live search for swimmers by their name.
     */
    private void filterSwimmersByName(String query) {
        swimmersModel.clear();
        List<Swimmer> swimmers = new ArrayList<>(resultsBySwimmer.keySet());
        swimmers.sort(Comparator.comparing(s -> s.name));

        for (Swimmer s : swimmers) {
            if (query.isEmpty() || s.name.toLowerCase().contains(query)) {
                swimmersModel.addElement(s);
            }
        }
    }

    /**
     * (Student) When a swimmer is clicked, remember who it is and refresh event list.
     */
    private void onSwimmerSelected(Swimmer swimmer) {
        selectedSwimmer = swimmer;
        updateEventListsForSwimmer();
        
        // (AI-ADDED) Update status
        if (swimmer != null) {
            statusLabel.setText("Selected: " + swimmer.name);
        }
    }

    /**
     * (Student) Show only events that the selected swimmer has actually swum.
     */
    private void updateEventListsForSwimmer() {
        anchorEventsModel.clear();
        targetEventsModel.clear();

        if (selectedSwimmer == null) {
            return;
        }

        List<Result> results = resultsBySwimmer.getOrDefault(selectedSwimmer, Collections.emptyList());
        Set<Event> events = new TreeSet<>(Comparator
                .comparingInt((Event e) -> e.distance)
                .thenComparing(e -> e.stroke)
                .thenComparing(e -> e.course));

        for (Result r : results) {
            events.add(r.event);
        }

        for (Event e : events) {
            anchorEventsModel.addElement(e);
            targetEventsModel.addElement(e);
        }
    }

    // ======== FILE CHOOSER / "DATABASE" LOADING (AI-ADDED) ========

    // (AI-ADDED) Let the user choose a single CSV file and load it in the background.
    private void onLoadCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "CSV Files", "csv"));
        chooser.setDialogTitle("Select Swim Data CSV File");
        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        setBusy(true, "Loading CSV: " + file.getName());

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    loadCsv(file.toPath());
                    filesLoaded = 1;
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() ->
                            showError("Failed to load CSV: " + e.getMessage()));
                }
                return null;
            }

            @Override
            protected void done() {
                setBusy(false, "Ready.");
                statusLabel.setText("Loaded CSV: " + file.getName() + 
                                  " (" + allResults.size() + " results)");
                showNotification("Data Loaded", 
                    "Successfully loaded " + allResults.size() + " results from " + file.getName());
            }
        };
        worker.execute();
    }

    /**
     * (AI-ADDED) Load from a "swim database", which in this project is a folder
     * full of CSV files. I try to match a filename like:
     *
     *   team_gender_year.csv   (with spaces in team replaced by underscores)
     *
     * If that specific file doesn't exist, I load all .csv files in the folder.
     */
    private void onLoadFromDatabase() {
        String team = tfTeam.getText().trim();
        String gender = Objects.toString(cbGender.getSelectedItem(), "M");
        String yearStr = tfYear.getText().trim();

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Swim Database Folder");
        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File folder = chooser.getSelectedFile();
        setBusy(true, "Loading from swim database...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    List<Result> temp = new ArrayList<>();
                    Path dir = folder.toPath();
                    filesLoaded = 0;

                    String expectedName = buildExpectedCsvName(team, gender, yearStr);
                    boolean loadedAny = false;

                    if (expectedName != null) {
                        Path specific = dir.resolve(expectedName);
                        if (Files.exists(specific)) {
                            loadCsvInto(temp, specific);
                            loadedAny = true;
                            filesLoaded++;
                        }
                    }

                    if (!loadedAny) {
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.csv")) {
                            for (Path p : stream) {
                                loadCsvInto(temp, p);
                                loadedAny = true;
                                filesLoaded++;
                            }
                        }
                    }

                    if (!loadedAny) {
                        throw new IOException("No .csv files found in folder: " + dir);
                    }

                    allResults.clear();
                    allResults.addAll(temp);
                    indexResults();
                    
                    // (AI-ADDED) Update in background
                    SwingUtilities.invokeLater(() -> {
                        updateQuickStats();
                        updateStatisticsTable();
                    });
                    
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() ->
                            showError("Failed to load from swim database: " + ex.getMessage()));
                }
                return null;
            }

            @Override
            protected void done() {
                rebuildSwimmerList();
                updateEventListsForSwimmer();
                setBusy(false, "Ready.");
                statusLabel.setText("Loaded " + allResults.size() + " results from " + 
                                  filesLoaded + " file(s)");
                showNotification("Database Loaded", 
                    "Successfully loaded " + allResults.size() + " results from database");
            }
        };
        worker.execute();
    }

    /**
     * (AI-ADDED) Build a filename like "Providence_M_2024.csv" based on the UI fields.
     * If the team or year are blank, I just return null and fall back to "load all csv".
     */
    private String buildExpectedCsvName(String team, String gender, String yearStr) {
        if (team.isEmpty() || yearStr.isEmpty()) {
            return null;
        }
        String cleanedTeam = team.trim().replaceAll("\\s+", "_");
        String cleanedGender = gender.trim();
        String cleanedYear = yearStr.trim();
        return cleanedTeam + "_" + cleanedGender + "_" + cleanedYear + ".csv";
    }

    // ======== MANUAL RESULT ENTRY HANDLER (AI-ADDED) ========

    // (AI-ADDED) Take data from the manual entry fields and add one Result.
    private void onAddManualResult() {
        String id = tfManualSwimmerId.getText().trim();
        String name = tfManualSwimmerName.getText().trim();
        String sex = Objects.toString(cbManualSex.getSelectedItem(), "M").trim();
        String birthYearStr = tfManualBirthYear.getText().trim();
        String distStr = tfManualDistance.getText().trim();
        String stroke = tfManualStroke.getText().trim();
        String course = tfManualCourse.getText().trim();
        String dateStr = tfManualDate.getText().trim();
        String timeStr = tfManualTime.getText().trim();

        if (id.isEmpty() || name.isEmpty() || birthYearStr.isEmpty()
                || distStr.isEmpty() || stroke.isEmpty()
                || course.isEmpty() || dateStr.isEmpty() || timeStr.isEmpty()) {
            showError("Please fill in all manual entry fields.");
            return;
        }

        int birthYear;
        int distance;
        double time;
        LocalDate date;
        try {
            birthYear = Integer.parseInt(birthYearStr);
        } catch (NumberFormatException e) {
            showError("Birth year must be an integer.");
            return;
        }
        try {
            distance = Integer.parseInt(distStr);
        } catch (NumberFormatException e) {
            showError("Distance must be an integer.");
            return;
        }
        try {
            time = Double.parseDouble(timeStr);
        } catch (NumberFormatException e) {
            showError("Time must be a number in seconds.");
            return;
        }
        try {
            date = LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            showError("Date must be in format YYYY-MM-DD.");
            return;
        }

        // Create a new swimmer and event for this result.
        Swimmer swimmer = new Swimmer(id, name, birthYear, sex);
        Event event = new Event(distance, stroke, course);
        Result result = new Result(swimmer, event, date, time);

        allResults.add(result);
        indexResults();
        rebuildSwimmerList();
        updateQuickStats();
        updateStatisticsTable();

        // Auto-select this swimmer in the list if possible.
        for (int i = 0; i < swimmersModel.size(); i++) {
            Swimmer s = swimmersModel.get(i);
            if (s.equals(swimmer)) {
                lstSwimmers.setSelectedIndex(i);
                lstSwimmers.ensureIndexIsVisible(i);
                break;
            }
        }
        updateEventListsForSwimmer();

        statusLabel.setText("Added manual result for " + name + ".");
        showNotification("Result Added", 
            "Added result for " + name + " in " + distance + " " + stroke + " (" + time + "s)");
        
        // Clear manual entry fields
        clearManualEntryFields();
    }
    
    // (AI-ADDED) Clear manual entry fields after successful addition
    private void clearManualEntryFields() {
        tfManualSwimmerId.setText("");
        tfManualSwimmerName.setText("");
        cbManualSex.setSelectedIndex(0);
        tfManualBirthYear.setText("");
        tfManualDistance.setText("");
        tfManualStroke.setText("");
        tfManualCourse.setText("");
        tfManualDate.setText("");
        tfManualTime.setText("");
    }

    // ======== PREDICTION LOGIC (Student) ========

    /**
     * (Student) Fired when the "Predict Time" button is pressed.
     */
    private void onPredict() {
        if (selectedSwimmer == null) {
            showError("Please select a swimmer first.");
            return;
        }
        Event anchor = lstAnchorEvents.getSelectedValue();
        Event target = lstTargetEvents.getSelectedValue();
        if (anchor == null || target == null) {
            showError("Please pick both an anchor event and a target event.");
            return;
        }

        setBusy(true, "Computing prediction…");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            String report;

            @Override
            protected Void doInBackground() {
                report = computePredictionReport(selectedSwimmer, anchor, target);
                return null;
            }

            @Override
            protected void done() {
                setBusy(false, "Ready.");
                outputArea.setText(report);
                outputArea.setCaretPosition(0);

                // (AI-ADDED) Also refresh the chart for this swimmer + anchor event.
                try {
                    updateChartFor(selectedSwimmer, anchor);
                } catch (Exception ex) {
                    System.err.println("Chart update failed: " + ex.getMessage());
                }
                
                // (AI-ADDED) Show prediction complete notification
                showNotification("Prediction Complete", 
                    "Generated prediction for " + selectedSwimmer.name);
            }
        };
        worker.execute();
    }

    /**
     * (Student) Build a plain-text explanation for the predicted time.
     */
    private String computePredictionReport(Swimmer swimmer, Event anchor, Event target) {
        List<Result> swimmerResults = resultsBySwimmer
                .getOrDefault(swimmer, Collections.emptyList());
        if (swimmerResults.isEmpty()) {
            return "No results available for " + swimmer.name;
        }

        List<Result> anchorResults = swimmerResults.stream()
                .filter(r -> r.event.equals(anchor))
                .sorted(Comparator.comparing(r -> r.date))
                .collect(Collectors.toList());

        if (anchorResults.size() < 2) {
            return "Not enough races in the anchor event (" + anchor +
                    ") to fit a trend line (need at least two races).";
        }

        LocalDate start = anchorResults.get(0).date;
        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        for (Result r : anchorResults) {
            long days = ChronoUnit.DAYS.between(start, r.date);
            xs.add((double) days);
            ys.add(r.timeSec);
        }

        double[] line = linearRegression(xs, ys);
        double slope = line[0];
        double intercept = line[1];

        double futureDays = 60.0;  // just a demo horizon
        double predictedAnchor = slope * futureDays + intercept;

        double cohortRatio = computeCohortRatio(anchor, target);

        double trainingScore =
                (sliderEffort.getValue() + sliderConsistency.getValue()) / 2.0;
        double finalPrediction = combinePredictions(predictedAnchor * cohortRatio, trainingScore);

        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                  PREDICTION REPORT - DataSplash                  ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════════╝\n\n");
        
        sb.append("SWIMMER:      ").append(swimmer.name).append("\n");
        sb.append("ID:           ").append(swimmer.id).append("\n");
        sb.append("GENDER/AGE:   ").append(swimmer.sex).append(", Born ").append(swimmer.birthYear).append("\n");
        sb.append("ANCHOR EVENT: ").append(anchor).append("\n");
        sb.append("TARGET EVENT: ").append(target).append("\n\n");
        
        sb.append("════════════════════════════════════════════════════════════════════\n");
        sb.append("TREND ANALYSIS:\n");
        sb.append("════════════════════════════════════════════════════════════════════\n");
        sb.append(String.format("  Trend line equation:  time = %.4f × days + %.4f\n", slope, intercept));
        sb.append(String.format("  R² correlation:        %.3f\n", calculateRSquared(xs, ys, slope, intercept)));
        sb.append(String.format("  Historical races:     %d\n", anchorResults.size()));
        sb.append(String.format("  Date range:           %s to %s\n", 
            anchorResults.get(0).date, anchorResults.get(anchorResults.size()-1).date));
        sb.append(String.format("  Time range:           %.2fs to %.2fs\n",
            Collections.min(ys), Collections.max(ys)));
        sb.append("\n");
        
        sb.append("════════════════════════════════════════════════════════════════════\n");
        sb.append("PREDICTION CALCULATION:\n");
        sb.append("════════════════════════════════════════════════════════════════════\n");
        sb.append(String.format("  Predicted anchor time (%.0f days):  %.2f s\n", futureDays, predictedAnchor));
        sb.append(String.format("  Cohort ratio (%s → %s):     %.3f\n", anchor, target, cohortRatio));
        sb.append(String.format("  Training score (effort/consistency): %.1f/5.0\n", trainingScore));
        sb.append(String.format("  Training adjustment factor:          %.3f\n", 
            trainingScore > 3.0 ? 1.0 - 0.01 * (trainingScore - 3.0) * 2.5 : 
                                  1.0 + 0.01 * (3.0 - trainingScore) * 2.5));
        sb.append("\n");
        
        sb.append("════════════════════════════════════════════════════════════════════\n");
        sb.append("FINAL PREDICTION:\n");
        sb.append("════════════════════════════════════════════════════════════════════\n");
        sb.append(String.format("  %-50s %.2f seconds\n", target + ":", finalPrediction));
        sb.append("\n");
        
        // (AI-ADDED) Add confidence indicator
        double confidence = calculateConfidence(anchorResults.size(), trainingScore, 
                                              Math.abs(slope), calculateRSquared(xs, ys, slope, intercept));
        sb.append("════════════════════════════════════════════════════════════════════\n");
        sb.append("CONFIDENCE METRICS:\n");
        sb.append("════════════════════════════════════════════════════════════════════\n");
        sb.append(String.format("  Prediction confidence:       %.1f%%\n", confidence * 100));
        sb.append(String.format("  Data quality:               %s\n", 
            anchorResults.size() >= 5 ? "Excellent" : 
            anchorResults.size() >= 3 ? "Good" : "Limited"));
        sb.append(String.format("  Trend strength:             %s\n",
            Math.abs(slope) > 0.01 ? "Strong" : "Stable"));
        sb.append("\n");
        
        sb.append("╔══════════════════════════════════════════════════════════════════╗\n");
        sb.append("║  Report generated: ").append(LocalDate.now()).append("                              ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════════╝\n");

        return sb.toString();
    }

    // (AI-ADDED) Calculate R-squared value for regression quality
    private double calculateRSquared(List<Double> xs, List<Double> ys, double slope, double intercept) {
        double meanY = ys.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double ssTotal = 0;
        double ssResidual = 0;
        
        for (int i = 0; i < xs.size(); i++) {
            double y = ys.get(i);
            double yPred = slope * xs.get(i) + intercept;
            ssTotal += Math.pow(y - meanY, 2);
            ssResidual += Math.pow(y - yPred, 2);
        }
        
        return 1 - (ssResidual / ssTotal);
    }

    // (AI-ADDED) Calculate prediction confidence based on multiple factors
    private double calculateConfidence(int dataPoints, double trainingScore, 
                                      double slopeMagnitude, double rSquared) {
        double dataConfidence = Math.min(dataPoints / 10.0, 1.0);
        double trainingConfidence = trainingScore / 5.0;
        double trendConfidence = Math.min(Math.abs(slopeMagnitude) * 100, 1.0);
        double rSquaredConfidence = rSquared;
        
        return (dataConfidence * 0.3 + trainingConfidence * 0.3 + 
                trendConfidence * 0.2 + rSquaredConfidence * 0.2);
    }

    /**
     * (Student) Standard least-squares linear regression for y = m x + b.
     */
    private double[] linearRegression(List<Double> xs, List<Double> ys) {
        int n = xs.size();
        if (n != ys.size() || n < 2) {
            throw new IllegalArgumentException("Need at least 2 data points for regression.");
        }

        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            double x = xs.get(i);
            double y = ys.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        double denom = n * sumXX - sumX * sumX;
        if (denom == 0) {
            throw new IllegalArgumentException("Regression denominator is zero.");
        }

        double m = (n * sumXY - sumX * sumY) / denom;
        double b = (sumY - m * sumX) / n;
        return new double[]{m, b};
    }

    /**
     * (Student) Compare average times for target vs anchor across the whole data set.
     */
    private double computeCohortRatio(Event anchor, Event target) {
        List<Result> anchorAll = resultsByEvent.getOrDefault(anchor, Collections.emptyList());
        List<Result> targetAll = resultsByEvent.getOrDefault(target, Collections.emptyList());
        if (anchorAll.isEmpty() || targetAll.isEmpty()) {
            return 1.0;
        }

        double avgAnchor = anchorAll.stream().mapToDouble(r -> r.timeSec).average().orElse(1.0);
        double avgTarget = targetAll.stream().mapToDouble(r -> r.timeSec).average().orElse(1.0);
        if (avgAnchor <= 0) return 1.0;
        return avgTarget / avgAnchor;
    }

    /**
     * (Student) Slightly adjusts the time based on the training sliders.
     */
    private double combinePredictions(double baseTime, double trainingScore) {
        if (Double.isNaN(baseTime)) {
            return Double.NaN;
        }

        // I let training move the time by up to roughly ±5%.
        double factor = 1.0;
        if (trainingScore > 3.0) {
            factor -= 0.01 * (trainingScore - 3.0) * 2.5;
        } else if (trainingScore < 3.0) {
            factor += 0.01 * (3.0 - trainingScore) * 2.5;
        }
        return baseTime * factor;
    }

    // ======== ENHANCED CHART UPDATE (AI-ADDED) ========

    // (AI-ADDED) Recompute anchor trend data for the chart (similar to earlier).
    private void updateChartFor(Swimmer swimmer, Event anchor) {
        if (swimmer == null || anchor == null) {
            chartPanel.setNoData();
            return;
        }

        List<Result> swimmerResults = resultsBySwimmer
                .getOrDefault(swimmer, Collections.emptyList());
        if (swimmerResults.isEmpty()) {
            chartPanel.setNoData();
            return;
        }

        List<Result> anchorResults = swimmerResults.stream()
                .filter(r -> r.event.equals(anchor))
                .sorted(Comparator.comparing(r -> r.date))
                .collect(Collectors.toList());

        if (anchorResults.size() < 2) {
            chartPanel.setNoData();
            return;
        }

        LocalDate start = anchorResults.get(0).date;
        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        for (Result r : anchorResults) {
            long days = ChronoUnit.DAYS.between(start, r.date);
            xs.add((double) days);
            ys.add(r.timeSec);
        }

        double[] line = linearRegression(xs, ys);
        double slope = line[0];
        double intercept = line[1];

        double futureDays = 60.0; // same horizon as computePredictionReport
        double predicted = slope * futureDays + intercept;

        chartPanel.setData(xs, ys, slope, intercept, futureDays, predicted, 
                          swimmer.name + " - " + anchor);
    }

    // ======== ENHANCED PERFORMANCE CHART PANEL (AI-ADDED) ========

    // (AI-ADDED/Student-edited) Advanced chart panel to show anchor times, regression line, and predicted point.
    private static class PerformanceChartPanel extends JPanel {

        private List<Double> xs = Collections.emptyList();
        private List<Double> ys = Collections.emptyList();
        private double slope = 0.0;
        private double intercept = 0.0;
        private double futureX = 0.0;
        private double futureY = 0.0;
        private String title = "";
        private boolean hasData = false;

        //Performance chart settings

        PerformanceChartPanel() {
            setPreferredSize(new Dimension(500, 350));
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 180, 255, 100), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
        }

        void setData(List<Double> xs, List<Double> ys,
                     double slope, double intercept,
                     double futureX, double futureY, String title) {
            this.xs = new ArrayList<>(xs);
            this.ys = new ArrayList<>(ys);
            this.slope = slope;
            this.intercept = intercept;
            this.futureX = futureX;
            this.futureY = futureY;
            this.title = title;
            this.hasData = !xs.isEmpty();
            repaint();
        }

        void setNoData() {
            this.hasData = false;
            this.title = "";
            repaint();
        }

        @Override //Paint component in one light mode for text
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            //If no data do nothing
            if (!hasData) {
                g2.setColor(getForeground());
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                String msg = "No data to display. Run a prediction first.";
                FontMetrics fm = g2.getFontMetrics();
                int x = (w - fm.stringWidth(msg)) / 2;
                int y = h / 2;
                g2.drawString(msg, x, y);
                g2.dispose();
                return;
            }

            int margin = 60;
            int x0 = margin;
            int y0 = h - margin;
            int xMax = w - margin;
            int yMax = margin;

            // Draw title
            g2.setColor(getForeground());
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            FontMetrics titleMetrics = g2.getFontMetrics();
            int titleX = (w - titleMetrics.stringWidth(title)) / 2;
            g2.drawString(title, titleX, 25);

            // Draw chart area with subtle background
            g2.setColor(new Color(255, 255, 255, 20));
            g2.fillRect(x0, yMax, xMax - x0, y0 - yMax);
            g2.setColor(new Color(200, 200, 200, 50));
            g2.drawRect(x0, yMax, xMax - x0, y0 - yMax);

            // Axes with arrows
            g2.setColor(new Color(180, 180, 180));
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(x0, y0, xMax, y0); // x axis
            g2.drawLine(x0, y0, x0, yMax); // y axis
            
            // Arrow heads
            g2.fillPolygon(new int[]{xMax, xMax-8, xMax-8}, new int[]{y0, y0-5, y0+5}, 3);
            g2.fillPolygon(new int[]{x0, x0-5, x0+5}, new int[]{yMax, yMax+8, yMax+8}, 3);

            double minX = 0.0;
            double maxX = Math.max(
                    futureX * 1.1, // Add 10% padding for future point
                    xs.isEmpty() ? 1.0 : xs.get(xs.size() - 1) * 1.1
            );

            if (maxX == minX) {
                maxX = minX + 1.0;
            }

            double minY = ys.stream().min(Double::compareTo).orElse(0.0) * 0.9; // 10% padding
            double maxY = Math.max(
                ys.stream().max(Double::compareTo).orElse(1.0) * 1.1,
                futureY * 1.1
            );

            if (maxY == minY) {
                maxY = minY + 1.0;
            }

            // Make effectively-final copies for use in lambdas
            final double minXf = minX;
            final double maxXf = maxX;
            final double minYf = minY;
            final double maxYf = maxY;
            final int x0f = x0;
            final int xMaxf = xMax;
            final int y0f = y0;
            final int yMaxf = yMax;

            DoubleFunction<Integer> mapX = v ->
                    (int) (x0f + (v - minXf) / (maxXf - minXf) * (xMaxf - x0f));
            DoubleFunction<Integer> mapY = v ->
                    (int) (y0f - (v - minYf) / (maxYf - minYf) * (y0f - yMaxf));

            // Grid lines for better readability
            g2.setColor(new Color(100, 100, 100, 50));
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 
                0, new float[]{2, 4}, 0));
            
            // Vertical grid lines
            for (double xVal = minX; xVal <= maxX; xVal += (maxX - minX) / 10) {
                int xGrid = mapX.apply(xVal);
                g2.drawLine(xGrid, y0, xGrid, yMax);
            }
            
            // Horizontal grid lines
            for (double yVal = minY; yVal <= maxY; yVal += (maxY - minY) / 10) {
                int yGrid = mapY.apply(yVal);
                g2.drawLine(x0, yGrid, xMax, yGrid);
            }
            
            g2.setStroke(new BasicStroke(1));

            // Regression line with gradient
            g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            GradientPaint lineGradient = new GradientPaint(
                x0f, y0f, new Color(100, 200, 255, 200),
                xMaxf, yMaxf, new Color(50, 150, 255, 200)
            );
            g2.setPaint(lineGradient);
            
            int rx1 = mapX.apply(minX);
            int ry1 = mapY.apply(slope * minX + intercept);
            int rx2 = mapX.apply(maxX);
            int ry2 = mapY.apply(slope * maxX + intercept);
            g2.drawLine(rx1, ry1, rx2, ry2);

            // Actual data points with connecting line
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < xs.size(); i++) {
                int px = mapX.apply(xs.get(i));
                int py = mapY.apply(ys.get(i));
                
                // Draw connecting line
                if (i > 0) {
                    int pxPrev = mapX.apply(xs.get(i - 1));
                    int pyPrev = mapY.apply(ys.get(i - 1));
                    g2.setColor(new Color(255, 200, 0, 150));
                    g2.drawLine(pxPrev, pyPrev, px, py);
                }
                
                // Draw point with glow effect
                g2.setColor(new Color(255, 255, 200, 100));
                g2.fillOval(px - 8, py - 8, 16, 16);
                
                g2.setColor(new Color(255, 200, 0));
                g2.fillOval(px - 6, py - 6, 12, 12);
                
                // Draw data point labels
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.BOLD, 9));
                String label = String.format("%.1f", ys.get(i));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, px - fm.stringWidth(label)/2, py - 8);
            }

            // Predicted point with special styling
            g2.setStroke(new BasicStroke(2));
            int fx = mapX.apply(futureX);
            int fy = mapY.apply(futureY);
            
            // Glow effect for prediction point
            g2.setColor(new Color(0, 255, 120, 50));
            for (int i = 0; i < 3; i++) {
                g2.fillOval(fx - 10 - i*2, fy - 10 - i*2, 20 + i*4, 20 + i*4);
            }
            
            // Main prediction point
            GradientPaint futureGradient = new GradientPaint(
                fx - 10, fy - 10, new Color(0, 255, 150),
                fx + 10, fy + 10, new Color(0, 200, 100)
            );
            g2.setPaint(futureGradient);
            g2.fillOval(fx - 10, fy - 10, 20, 20);
            
            g2.setColor(Color.WHITE);
            g2.drawOval(fx - 10, fy - 10, 20, 20);
            
            // Prediction label
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            String predLabel = String.format("Predicted: %.2fs", futureY);
            FontMetrics predFm = g2.getFontMetrics();
            g2.setColor(new Color(0, 255, 120));
            g2.drawString(predLabel, fx + 12, fy - 12);

            // Axis labels with units
            g2.setColor(getForeground());
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString("Days from first race", (x0 + xMax)/2 - 40, h - 15);
            
            g2.rotate(-Math.PI / 2);
            g2.drawString("Time (seconds)", -h/2 - 40, 15);
            g2.rotate(Math.PI / 2);

            // Legend for chart elements
            int legendY = 45;
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            
            // Historical data legend
            g2.setColor(new Color(255, 200, 0));
            g2.fillRect(w - 120, legendY, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawRect(w - 120, legendY, 12, 12);
            g2.setColor(getForeground());
            g2.drawString("Historical", w - 100, legendY + 10);
            
            // Trend line legend
            legendY += 20;
            g2.setColor(new Color(100, 200, 255));
            g2.setStroke(new BasicStroke(3));
            g2.drawLine(w - 120, legendY + 6, w - 108, legendY + 6);
            g2.setColor(getForeground());
            g2.drawString("Trend", w - 100, legendY + 10);
            
            // Prediction legend
            legendY += 20;
            g2.setColor(new Color(0, 255, 120));
            g2.fillOval(w - 120, legendY, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawOval(w - 120, legendY, 12, 12);
            g2.setColor(getForeground());
            g2.drawString("Prediction", w - 100, legendY + 10);

            g2.dispose();
        }
    }

    // ======== HELPER METHODS (AI-ADDED) ========

    // (AI-ADDED) Create notification popup for user feedback
    private void showNotification(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            JLabel icon = new JLabel("🔔");
            icon.setFont(new Font("Segoe UI", Font.PLAIN, 24));
            
            JTextArea text = new JTextArea(message);
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setOpaque(false);
            text.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            
            panel.add(icon, BorderLayout.WEST);
            panel.add(new JScrollPane(text), BorderLayout.CENTER);
            
            JOptionPane.showMessageDialog(this, panel, title, 
                JOptionPane.INFORMATION_MESSAGE);
        });
    }

    // (AI-ADDED) Create application icon for window title bar
    private Image createAppIcon() {
        BufferedImage icon = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                           RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw wave background
        GradientPaint bgGradient = new GradientPaint(
            0, 0, new Color(30, 60, 120),
            64, 64, new Color(10, 30, 60)
        );
        g2.setPaint(bgGradient);
        g2.fillRect(0, 0, 64, 64);
        
        // Draw waves
        g2.setColor(new Color(100, 180, 255, 200));
        for (int i = 0; i < 3; i++) {
            int y = 40 + i * 6;
            g2.fillOval(10, y, 44, 20);
            g2.setColor(new Color(100, 180, 255, 150 - i * 30));
        }
        
        // Draw swimmer silhouette
        g2.setColor(Color.WHITE);
        g2.fillOval(22, 15, 20, 20); // Head
        g2.fillRect(27, 35, 10, 20); // Body
        g2.fillRect(20, 35, 7, 15); // Arm
        g2.fillRect(37, 35, 7, 15); // Arm
        
        g2.dispose();
        return icon;
    }

    // (AI-ADDED) Update comparison events based on selected swimmers
    private void updateComparisonEvents(JComboBox<Swimmer> cb1, JComboBox<Swimmer> cb2, 
                                       JComboBox<Event> cbEvent) {
        Swimmer s1 = (Swimmer) cb1.getSelectedItem();
        Swimmer s2 = (Swimmer) cb2.getSelectedItem();
        
        if (s1 == null || s2 == null) return;
        
        Set<Event> events1 = resultsBySwimmer.getOrDefault(s1, Collections.emptyList())
                .stream().map(r -> r.event).collect(Collectors.toSet());
        Set<Event> events2 = resultsBySwimmer.getOrDefault(s2, Collections.emptyList())
                .stream().map(r -> r.event).collect(Collectors.toSet());
        
        events1.retainAll(events2); // Keep only common events
        
        cbEvent.removeAllItems();
        for (Event e : events1) {
            cbEvent.addItem(e);
        }
    }

    // (AI-ADDED) Show comparison results between two swimmers
    private void showComparisonResults(Swimmer s1, Swimmer s2, Event event) {
        List<Result> results1 = resultsBySwimmer.getOrDefault(s1, Collections.emptyList())
                .stream().filter(r -> r.event.equals(event))
                .sorted(Comparator.comparing(r -> r.date))
                .collect(Collectors.toList());
        
        List<Result> results2 = resultsBySwimmer.getOrDefault(s2, Collections.emptyList())
                .stream().filter(r -> r.event.equals(event))
                .sorted(Comparator.comparing(r -> r.date))
                .collect(Collectors.toList());
        
        //Append, etc. 
        StringBuilder sb = new StringBuilder();
        sb.append("COMPARISON: ").append(s1.name).append(" vs ").append(s2.name).append("\n");
        sb.append("EVENT: ").append(event).append("\n\n");
        
        if (results1.isEmpty() || results2.isEmpty()) {
            sb.append("Insufficient data for comparison.");
        } else {
            double avg1 = results1.stream().mapToDouble(r -> r.timeSec).average().orElse(0);
            double avg2 = results2.stream().mapToDouble(r -> r.timeSec).average().orElse(0);
            double best1 = results1.stream().mapToDouble(r -> r.timeSec).min().orElse(0);
            double best2 = results2.stream().mapToDouble(r -> r.timeSec).min().orElse(0);
            
            sb.append(String.format("%-20s %-20s %-20s\n", "Metric", s1.name, s2.name));
            sb.append(String.format("%-20s %-20.2f %-20.2f\n", "Average Time:", avg1, avg2));
            sb.append(String.format("%-20s %-20.2f %-20.2f\n", "Best Time:", best1, best2));
            sb.append(String.format("%-20s %-20d %-20d\n", "Races:", results1.size(), results2.size()));
            
            if (avg1 < avg2) {
                sb.append(String.format("\n%s is faster by %.2f seconds on average.", 
                    s1.name, avg2 - avg1));
            } else {
                sb.append(String.format("\n%s is faster by %.2f seconds on average.", 
                    s2.name, avg1 - avg2));
            }
        }
        
        JOptionPane.showMessageDialog(this, sb.toString(), "Comparison Results", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    // ======== MAIN (Student, with AI look-and-feel tweak) ========

    public static void main(String[] args) {
        // (AI-ADDED) Set system look and feel for native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // (AI-ADDED) Customize UI defaults for better appearance
            UIManager.put("Button.arc", 20);
            UIManager.put("Component.arc", 20);
            UIManager.put("ProgressBar.arc", 20);
            UIManager.put("TextComponent.arc", 10);
            
            UIManager.put("Button.background", new Color(100, 180, 255));
            UIManager.put("Button.foreground", Color.BLACK);
            UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 12));
            
            UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 12));
            UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 12));
            UIManager.put("TextArea.font", new Font("Segoe UI", Font.PLAIN, 12));
            
        } catch (Exception e) {
            System.err.println("Failed to set system look and feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            DataSplash app = new DataSplash();
            app.setVisible(true);
            
            // (AI-ADDED) Show welcome message
            SwingUtilities.invokeLater(() -> {
                app.showNotification("Welcome to DataSplash", 
                    "Welcome to DataSplash v2.0!\n\n" +
                    "To get started:\n" +
                    "1. Load swim data using the buttons above\n" +
                    "2. Select a swimmer from the list\n" +
                    "3. Choose events and adjust training factors\n" +
                    "4. Click Predict Time to generate forecasts\n\n" +
                    "Check the Help menu for detailed instructions.");
            });
        });
    }
}