# 🌊 DataSplash  
### Swim Time Prediction Tool

**Honors Java / Data Structures Digital Portfolio (2025–2026)**  
Charlotte Latin School

DataSplash is a Java-based application designed to help competitive swimmers
predict future race times when they are unsure what goal time to aim for.
By analyzing historical swim meet data and applying statistical trends,
DataSplash provides a realistic, data-driven performance estimate.

🔗 **Repository:** https://github.com/Origamibuilder/Ashri-Nishant-Portfolio  
👩‍🏫 **Instructor Access:** Mrs. Morrow (collaborator)

---

## 📌 Quick Project Summary (Submission Version)

**DataSplash** analyzes historical swim meet data using Java data structures
and linear regression to predict future race performance. Swimmers select a
known event, a target event, and training factors, and the program generates
both a written prediction and a visual performance chart.

This project demonstrates file I/O, data structures, GUI programming, and
statistical reasoning in a real-world athletic context.

---

## 🧠 Project Motivation

Many swimmers experience pre-race anxiety when they do not have a clear goal
time for an event. DataSplash addresses this problem by turning past meet data
into meaningful predictions, giving swimmers a starting point for goal-setting
and reducing uncertainty before competition.

This project blends **computer science**, **data analysis**, and **sports
performance psychology** into a practical and interactive tool.

---

## ⚙️ How It Works

### 1️⃣ Data Input
- Load a **single CSV file** or an entire **folder of CSV files**
- Files act as a swim database (similar in structure to SwimCloud data)

### 2️⃣ Swimmer & Event Selection
- Search and select a swimmer
- Choose:
  - **Anchor Event** – an event with known past performance
  - **Target Event** – the event to predict

### 3️⃣ Prediction Engine
- Applies **linear regression** to historical race data
- Compares swimmer performance to others of similar caliber
- Adjusts predictions using:
  - Training effort
  - Training consistency

### 4️⃣ Output & Visualization
- Generates a detailed text-based prediction report
- Displays an interactive chart showing:
  - Historical race results
  - Trend line
  - Predicted future time

---

## ✨ Key Features

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

---

## 🗂️ File Structure

```text
DataSplash.java        → Main Java application
swimdb/
 └── fakeData.csv      → Example dataset for testing
README.md              → Project documentation
