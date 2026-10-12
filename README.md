# INTELLECTUALS HOTEL: Billing App

A small hotel billing desktop app. A receptionist logs in, enters a guest's stay,
sees the bill calculate live, saves it, and can view, print or delete receipts.
Written in Java 21 with Swing, organised as **MVC**.

The hotel name shown on every screen is set in one place: `NAME` in `view/AppInfo.java`.

## Requirements

- **JDK 21** (the build asks for exactly this version)
- Gradle is **not** required: the included `gradlew` / `gradlew.bat` downloads the right version on first use
  (needs internet the first time only)
- IntelliJ IDEA (Community is fine) or any terminal

## Running it

**In IntelliJ**

1. Open the `LodgeBillingLab` folder (File > Open) and let Gradle finish syncing.
2. Open `src/main/java/lodge/Main.java` and press the green arrow next to `main`.
3. To run the tests: right-click `src/test/java` > Run 'All Tests'.

**In a terminal** (from the project folder)

```
./gradlew run        # start the app   (Windows: gradlew.bat run)
./gradlew test       # run the tests
```

**Practice logins**

| Username | Password |
|---|---|
| `admin` | `lodge123` |
| `reception` | `welcome1` |

Three wrong attempts closes the app.

## Project layout (where every file goes)

```
LodgeBillingLab/
├── build.gradle                 build settings (Java 21, JUnit 5)
├── settings.gradle              project name
├── gradlew, gradlew.bat         Gradle wrapper (run Gradle without installing it)
├── gradle/wrapper/              wrapper files, keep these
├── .gitignore, .gitattributes   Git housekeeping
├── README.md
└── src/
    ├── main/
    │   ├── java/lodge/
    │   │   ├── Main.java                      entry point and wiring
    │   │   ├── model/                         MODEL
    │   │   │   ├── RoomType.java
    │   │   │   ├── Bill.java
    │   │   │   ├── BillingCalculator.java
    │   │   │   ├── BillRepository.java
    │   │   │   ├── AuthService.java
    │   │   │   ├── ReceiptFormatter.java
    │   │   │   └── BillCsvExporter.java
    │   │   ├── view/                          VIEW
    │   │   │   ├── MainWindow.java
    │   │   │   ├── LoginDialog.java
    │   │   │   ├── ReceiptDialog.java
    │   │   │   ├── ReceiptPrintable.java
    │   │   │   ├── BillTableModel.java
    │   │   │   ├── AppInfo.java
    │   │   │   ├── Logo.java
    │   │   │   ├── Theme.java
    │   │   │   ├── RoundButton.java
    │   │   │   ├── RoundedPanel.java
    │   │   │   ├── Icons.java
    │   │   │   └── FileDialogs.java
    │   │   └── controller/                    CONTROLLER
    │   │       ├── BillController.java
    │   │       └── InputParser.java
    │   └── resources/
    │       └── logo.png                       YOUR logo goes here
    └── test/java/lodge/
        ├── model/        BillingCalculatorTest, BillRepositoryTest, AuthServiceTest, ReceiptAndExportTest
        └── controller/   InputParserTest
```

Every file's `package` line must match its folder (for example `package lodge.view;` inside `lodge/view/`).
If IntelliJ says "package name does not match the expected package", the file is in the wrong folder.

## How MVC is used here

| Part | Job | Rule it follows |
|---|---|---|
| **Model** (`lodge.model`) | The data and the business rules: prices, discount, saving to disk, login checks, receipt text, CSV | Contains **no Swing code**, so it can be tested without a window |
| **View** (`lodge.view`) | Everything the user sees: windows, buttons, table, logo, printing | Contains **no business rules**. It only draws things and reports what the user did |
| **Controller** (`lodge.controller`) | Reacts to the user, asks the model to do the work, tells the view what to show | The only part that knows about both sides |

`Main` builds the model, then connects a view to a controller each time someone logs in.

**How one action flows.** The user types `6` in Nights:

1. **View:** the Nights field reports the change to its listener.
2. **Controller:** `BillController.onFormChanged()` reads the form text and checks it with `InputParser`.
3. **Model:** `BillingCalculator.calculate(...)` returns the room charge, discount, total and balance.
4. **View:** `MainWindow.showSummary(...)` updates the Live bill labels.

When a bill is saved or deleted, `BillRepository` notifies its listeners, so the table and the footer
totals refresh by themselves. The table keeps no second copy of the data.

## The billing rules

- Room charge = price per night x nights (Standard K350, Deluxe K550, Family K750)
- 5 nights or more: 5% off the **room charge only**
- Extras are never discounted, and there is no tax
- Total = room charge - discount + extras
- Balance = total - paid. A negative balance is shown as **Change due**

Money uses `BigDecimal`, not `double`, so cents never drift.

## Features

- Live bill that recalculates as you type (there is no Calculate button), with red outlines on bad input
- Bills are saved automatically and are still there next time you open the app
- Sortable table (click a column header) and a search box for guest, bill number or room
- Receipt window with **Print** and **Save as text file**
- Export every bill to a CSV file that opens in Excel
- Footer totals: billed, collected and outstanding
- Warning before an unsaved form is thrown away (New guest, Log out, Exit)
- Passwords are stored as salted PBKDF2 hashes, never as plain text

**Input limits:** nights 1 to 365, amounts up to K10,000,000 with at most 2 decimals,
guest name up to 60 characters, notes up to 300.

**Keyboard shortcuts**

| Keys | Action |
|---|---|
| Ctrl+N | New guest |
| Ctrl+S | Save bill |
| Ctrl+R | View receipt of selected bill (or double-click a row, or press Enter on it) |
| Ctrl+D or Delete | Delete selected bill |
| Ctrl+E | Export to CSV |
| Ctrl+L | Log out |
| Ctrl+Q | Exit |
| F1 | About |

## Adding your logo

1. In IntelliJ, right-click `src/main` > New > Directory > `resources` (skip this if it already exists).
2. Copy your image into it and name it exactly `logo.png` (`logo.jpg`, `logo.jpeg` and `logo.gif` also work).
3. Run the app again.

The logo then appears in the blue header bar (on a small white badge), on the login screen,
at the top of the receipt window, at the top of the printed receipt, and as the window icon.
A PNG with a transparent background looks best, and about 300 px wide or more stays sharp.
You do not need to resize it. With no logo file the app simply runs without one.

To get a PDF receipt with the logo, choose a "Save as PDF" printer in the Print dialog.
The text file version cannot contain the logo.

## Where the data is saved

Bills are saved in your home folder:

- Windows: `C:\Users\<you>\.lodge-billing-lab\bills.tsv`
- Mac / Linux: `~/.lodge-billing-lab/bills.tsv`

To start fresh, close the app and delete that file. If the file is ever damaged, the app skips the
bad lines and keeps a `bills.tsv.backup` copy next to it.

## Troubleshooting

| Problem | Fix |
|---|---|
| "No matching toolchains found" or a Java version error | Install JDK 21: File > Project Structure > SDKs > + > Download JDK > version 21. Then Settings > Build Tools > Gradle > set **Gradle JVM** to 21 |
| `package ... does not match the expected package` | The file is in the wrong folder. Move it to the folder named in its `package` line |
| `cannot find symbol` for `Logo`, `Theme`, `BillController`... | The file is missing or in the wrong folder. Compare with the layout above |
| Tests are red or "cannot resolve org.junit" | Make sure `build.gradle` has the JUnit lines, then Gradle > Reload All Gradle Projects |
| `gradlew` fails or says "Could not find or load main class GradleWrapperMain" | The `gradle/wrapper/` folder (with `gradle-wrapper.jar`) is missing. Restore it |
| Logo does not show | The file must be named `logo.png` and sit in `src/main/resources`. Rebuild and run again |
| Text looks too small or blurry on a high-resolution screen | Run with the VM option `-Dsun.java2d.uiScale=1.5` (Run > Edit Configurations > Modify options > Add VM options) |

## Ideas for later

- Edit a saved bill instead of deleting and re-entering it
- Show the Kwacha price list from a settings file instead of the `RoomType` enum
- Store bills in a real database (the `BillRepository` is the only class that would change)
