#  DataSplash  
### Swim Time Prediction Tool

**Honors Java / Data Structures Digital Portfolio (2025–2026)**  
Charlotte Latin School

DataSplash is a Java-based application designed to help competitive swimmers
predict future race times when they are unsure what goal time to aim for.
By analyzing historical swim meet data and applying statistical trends,
DataSplash provides a realistic, data-driven performance estimate.

**Repository:** https://github.com/Origamibuilder/Ashri-Nishant-Portfolio  
**Instructor Access:** Mrs. Morrow (collaborator)


## Quick Project Summary 

**DataSplash** analyzes historical swim meet data using Java data structures
and linear regression to predict future race performance. Swimmers select a
known event, a target event, and training factors, and the program generates
both a written prediction and a visual performance chart.

This project demonstrates file I/O, data structures, GUI programming, and
statistical reasoning in a real-world athletic context.

## Project Motivation

Many swimmers experience pre-race anxiety when they do not have a clear goal
time for an event. DataSplash addresses this problem by turning past meet data
into meaningful predictions, giving swimmers a starting point for goal-setting
and reducing uncertainty before competition.

This project blends **computer science**, **data analysis**, and **sports
performance psychology** into a practical and interactive tool.

## How It Works

### Data Input
- Load a **single CSV file** or an entire **folder of CSV files**
- Files act as a swim database (similar in structure to SwimCloud data)

### Swimmer & Event Selection
- Search and select a swimmer
- Choose:
  - **Anchor Event** – an event with known past performance
  - **Target Event** – the event to predict

### Prediction Engine
- Applies **linear regression** to historical race data
- Compares swimmer performance to others of similar caliber
- Adjusts predictions using:
  - Training effort
  - Training consistency

### Output & Visualization
- Generates a detailed text-based prediction report
- Displays an interactive chart showing:
  - Historical race results
  - Trend line
  - Predicted future time

---

### Usage Instructions

### 1. Launch the Application
Compile and run DataSplash.java.
The main window will open with the swimmer list on the left and prediction tools on the right.

### 2. Load Swim Data
You can load data in two ways:
Option A: Load a Single CSV File
Click “Load Single CSV”.
Select a CSV file containing swim race data.
The program will automatically parse and organize the results.
Option B: Load a Database Folder
Enter a team name, gender, and year (optional).
Click “Load Swim Database” and select a folder containing CSV files.
DataSplash will load the matching file or all CSV files in the folder.
Required CSV Format:
swimmerId,name,birthYear,sex,distance,stroke,course,date,timeSeconds
Example:
12345,Jane Doe,2007,F,100,Free,SCY,2024-02-10,56.23
   
### 3. Select a Swimmer
Choose a swimmer from the list on the left.
Use the search bar to filter swimmers by name.
Once selected, available events for that swimmer will appear automatically.

### 4. Choose Events
Select an Anchor Event (a known performance).
Select a Target Event (the event you want to predict).
Both events must be selected to run a prediction.

### 5. Adjust Training Factors
Use the Effort slider to represent training intensity.
Use the Consistency slider to represent training regularity.
Values range from 1 (low) to 5 (high).

### 6. Run a Prediction
Click “Predict Time”.
DataSplash will:
Analyze historical race trends
Apply statistical regression
Adjust the prediction based on training factors
Results appear as:
A text-based report
A visual performance chart

### 7. View Statistics and Visualizations
Use the Statistics tab to see dataset summaries.
View the Performance Chart to visualize trends and predicted results.
Access additional tools such as swimmer comparison from the Tools menu.

### 8. Add Results Manually (Optional)
Enter swimmer and race details in the Manual Entry panel.
Click “Add Result” to immediately include the data.
The swimmer list and predictions update automatically.

### 10. Export Results
Use File → Export Prediction Report to save results as a text file.

### 11. Theme & Accessibility
Click “Switch Theme” in the bottom bar to toggle between dark and light modes.
Progress indicators and status messages guide users during long operations.



## Key Features

- Java Swing GUI with event-driven programming
- CSV file parsing and validation
- Core data structures:
  - `ArrayList`
  - `HashMap`
  - `Set`
- Linear regression and statistical analysis
- Manual data entry for custom race results
- Interactive performance chart
- Dark / light theme toggle
- Robust error handling and user feedback



## File Structure

DataSplash.java        → Main Java application
swimdb/
 └── fakeData.csv      → Example dataset for testing
README.md              → Project documentation

# Academic Integrity & AI Disclosure

This project is my original work unless otherwise noted. Any code explicitly marked with:

java
// (AI-ADDED)
was created or refined with assistance from an AI tool. All architectural decisions, logic integration, testing, and final implementation reflect my own understanding and work. External resources are cited below.


# References (MLA)

"Staying in Good Nervous before Your Races." USA Swimming, 2018.
https://www.usaswimming.org/news/2018/12/11/staying-in-good-nervous-before-your-races.

Narducci, Dusty M., et al. "Correlation of Pre-Race Anxiety Using the Generalized Anxiety Disorder 2-Item Scale." Psychiatric Quarterly, 2021.
https://doi.org/10.1007/s11126-021-09964-1.

"How to Mentally Prepare for Your Next Big Swim Meet." Elsmore Swim Shop, 2025.
https://elsmoreswim.com/pages/how-to-mentally-prepare-for-your-next-big-swim-meet.


# Author

Nishant Ashri
Junior, Charlotte Latin School
Interests include computer science, engineering, artificial intelligence, competitive swimming, speech & debate, and music (clarinet).


