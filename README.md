INTELLECTUALS HOTEL: Billing App
A small hotel billing desktop app. A receptionist logs in, enters a guest's stay, sees the bill calculate live, saves it, and can view, print or delete receipts. Accounts are protected by hashed passwords, roles, and a single officiating code issued by an administrator. Written in Java 21 with Swing, organised as MVC.

The hotel name shown on every screen is set in one place: NAME in view/AppInfo.java.

Requirements
- JDK 21 (the build asks for exactly this version)
- Gradle is not required: the included gradlew / gradlew.bat downloads the right version on first use (needs internet the first time only)
- IntelliJ IDEA (Community is fine) or any terminal
- SQLite is bundled via the JDBC driver — nothing to install

Libraries used
- org.xerial:sqlite-jdbc:3.46.0.0 — talks to the SQLite database file
- org.slf4j:slf4j-api:2.0.13 + ch.qos.logback:logback-classic:1.5.6 — logging backend used by the SQLite driver. Silences the "no SLF4J binding" warning.
- org.junit.jupiter:junit-jupiter:5.10.2 — unit tests

Running it

In IntelliJ
1. Open the LodgeBillingLab folder (File > Open) and let Gradle finish syncing.
2. Open src/main/java/lodge/Main.java and press the green arrow next to main.
3. To run the tests: right-click src/test/java > Run 'All Tests'.

In a terminal (from the project folder)
./gradlew run        # start the app   (Windows: gradlew.bat run)
./gradlew test       # run the tests

First run — the officiating code
On the very first launch, the app asks you to choose an officiating code (minimum 6 characters, repeat to confirm). This code is required to create any new account, to reset a password, and to change the officiating code itself.

The code is stored only as a salted PBKDF2 hash (120,000 iterations, SHA-256) inside the settings table. Nobody — not even you, reading the database — can recover the plain code from the file. If you forget it, the only recovery is to delete lodge.db and start again.

Practice logins
The first time the app runs, two accounts are created automatically:

Username   | Password  | Role
admin      | lodge123  | ADMIN
reception  | welcome1  | RECEPTION

Three wrong attempts closes the app.

Accounts and access control
Access to the app is guarded in four layers.

1. The officiating code
Nobody can create an account without it. It is verified against the PBKDF2 hash in settings before any insert into users.

2. Hashed passwords
Passwords are never stored. Each users row has:
- salt: 16 random bytes, unique per user
- hash: 256-bit PBKDF2-HMAC-SHA256 of the password (120,000 iterations)

Login works by re-hashing the typed password with the stored salt and comparing with a constant-time comparison.

3. The active flag
Every user row has active = 1 or 0. Setting it to 0 blocks that user from logging in even with the correct password. Only admins can toggle it.

4. Roles
users.role is ADMIN or RECEPTION.
- ADMIN — sees the Admin menu. Can open the user manager, deactivate or reactivate accounts, reset passwords, and change the officiating code.
- RECEPTION — no Admin menu. Can use the billing features normally.

Creating additional accounts
Anyone can press Create account on the login screen. The dialog asks for:
- Username
- Display name
- Role (ADMIN / RECEPTION)
- Password (min 6 characters) + confirm
- Officiating code  <-- the gatekeeper

If the officiating code is wrong, the account is not created.

Admin menu (admins only)
Admin > Manage users... opens a dialog that lists every account (ID, username, display name, role, active flag). From there an admin can:
- Activate / Deactivate — flips the active flag. An admin cannot deactivate their own account.
- Reset password — sets a new password for the selected user.
- Change officiating code — requires the current code first.

The database
All data lives in a single SQLite file:
Windows: C:\Users\<you>\.lodge-billing-lab\lodge.db
Mac / Linux: ~/.lodge-billing-lab/lodge.db

The schema is created automatically on first run.

Table users
- id INTEGER PRIMARY KEY AUTOINCREMENT
- username TEXT NOT NULL UNIQUE
- display_name TEXT NOT NULL
- role TEXT NOT NULL            (ADMIN or RECEPTION)
- salt BLOB NOT NULL            (16 random bytes)
- hash BLOB NOT NULL            (32 bytes, PBKDF2)
- active INTEGER NOT NULL       (1 = enabled, 0 = blocked)
- created_at TEXT NOT NULL      (ISO timestamp)

Table settings
- key TEXT PRIMARY KEY          (e.g. officiating_salt, officiating_hash)
- value BLOB NOT NULL           (binary value)

Table bills
- id TEXT PRIMARY KEY           (e.g. B001)
- guest TEXT NOT NULL
- room TEXT NOT NULL            (STANDARD, DELUXE, FAMILY)
- nights INTEGER NOT NULL
- extras TEXT NOT NULL          (decimal stored as text)
- paid TEXT NOT NULL            (decimal stored as text)
- notes TEXT NOT NULL DEFAULT ''
- room_charge TEXT NOT NULL
- discount TEXT NOT NULL
- total TEXT NOT NULL
- balance TEXT NOT NULL         (negative = change due)
- created_at TEXT NOT NULL
- served_by TEXT NOT NULL       (username of receptionist)

Indexes exist on bills.guest and bills.created_at.

Money is stored as text, not as REAL. Decimals like 0.10 would drift if stored as binary floats. Every amount is a BigDecimal end-to-end.

Project layout (where every file goes)
LodgeBillingLab/
├── build.gradle                 build settings (Java 21, SQLite, JUnit 5, Logback)
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
    │   │   │   ├── User.java                  one account record (NEW)
    │   │   │   ├── Database.java              all SQL lives here (NEW)
    │   │   │   ├── AuthService.java           login, signup, roles, codes
    │   │   │   ├── RoomType.java
    │   │   │   ├── Bill.java
    │   │   │   ├── BillingCalculator.java
    │   │   │   ├── BillRepository.java        in-memory cache over Database
    │   │   │   ├── ReceiptFormatter.java
    │   │   │   └── BillCsvExporter.java
    │   │   ├── view/                          VIEW
    │   │   │   ├── MainWindow.java
    │   │   │   ├── LoginDialog.java
    │   │   │   ├── SignUpDialog.java          account creation (NEW)
    │   │   │   ├── AdminDialog.java           user management (NEW)
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

Every file's package line must match its folder (for example package lodge.view; inside lodge/view/). If IntelliJ says "package name does not match the expected package", the file is in the wrong folder.

How MVC is used here
Part                    | Job                                                                                          | Rule it follows
Model (lodge.model)     | The data and the business rules: prices, discount, saving to disk, login checks, receipt text, CSV | Contains no Swing code, so it can be tested without a window
View (lodge.view)       | Everything the user sees: windows, buttons, table, logo, printing                             | Contains no business rules. It only draws things and reports what the user did
Controller (lodge.controller) | Reacts to the user, asks the model to do the work, tells the view what to show          | The only part that knows about both sides

Main builds the model, then connects a view to a controller each time someone logs in.

How one action flows. The user types 6 in Nights:
1. View: the Nights field reports the change to its listener.
2. Controller: BillController.onFormChanged() reads the form text and checks it with InputParser.
3. Model: BillingCalculator.calculate(...) returns the room charge, discount, total and balance.
4. View: MainWindow.showSummary(...) updates the Live bill labels.

When a bill is saved or deleted, BillRepository writes to SQLite and notifies its listeners, so the table and the footer totals refresh by themselves. The table keeps no second copy of the data.

The billing rules
- Room charge = price per night x nights (Standard K350, Deluxe K550, Family K750)
- 5 nights or more: 5% off the room charge only
- Extras are never discounted, and there is no tax
- Total = room charge - discount + extras
- Balance = total - paid. A negative balance is shown as Change due
- Money uses BigDecimal, not double, so cents never drift.

Features
- Live bill that recalculates as you type (there is no Calculate button), with red outlines on bad input
- Bills are saved automatically to SQLite and are still there next time you open the app
- Sortable table (click a column header) and a search box for guest, bill number or room
- Receipt window with Print and Save as text file
- Export every bill to a CSV file that opens in Excel (with BOM and formula-injection protection)
- Footer totals: billed, collected and outstanding
- Warning before an unsaved form is thrown away (New guest, Log out, Exit)
- Passwords are stored as salted PBKDF2 hashes, never as plain text
- Officiating-code gate on account creation
- Admin-only user management (deactivate, reset password, change officiating code)
- Roles — ADMIN sees extra menu items, RECEPTION does not

Input limits: nights 1 to 365, amounts up to K10,000,000 with at most 2 decimals, guest name up to 60 characters, notes up to 300.

Keyboard shortcuts
Keys                 | Action
Ctrl+N               | New guest
Ctrl+S               | Save bill
Ctrl+R               | View receipt of selected bill (or double-click a row, or press Enter on it)
Ctrl+D or Delete     | Delete selected bill
Ctrl+E               | Export to CSV
Ctrl+L               | Log out
Ctrl+Q               | Exit
Ctrl+M               | Manage users (admins only)
F1                   | About

Adding your logo
1. In IntelliJ, right-click src/main > New > Directory > resources (skip this if it already exists).
2. Copy your image into it and name it exactly logo.png (logo.jpg, logo.jpeg and logo.gif also work).
3. Run the app again.

The logo then appears in the blue header bar (on a small white badge), on the login screen, at the top of the receipt window, at the top of the printed receipt, and as the window icon. A PNG with a transparent background looks best, and about 300 px wide or more stays sharp. You do not need to resize it. With no logo file the app simply runs without one.

To get a PDF receipt with the logo, choose a "Save as PDF" printer in the Print dialog. The text file version cannot contain the logo.

Where the data is saved
- Windows: C:\Users\<you>\.lodge-billing-lab\lodge.db
- Mac / Linux: ~/.lodge-billing-lab/lodge.db

To start fresh, close the app and delete that file. It will be recreated on the next launch, including the default admin and reception accounts, and it will ask you for a new officiating code.

Note: the older bills.tsv file from version 1 is no longer read. You may delete it.

Inspecting the database
DB Browser for SQLite is the easiest way. Free, GUI, no install needed (portable version available): https://sqlitebrowser.org/dl/

1. Launch DB Browser for SQLite.
2. Click Open Database.
3. Paste the full path directly into the File name field (dot-folders are hidden by default):
   Windows: C:\Users\<you>\.lodge-billing-lab\lodge.db
   Mac/Linux: ~/.lodge-billing-lab/lodge.db
4. Click Open.
5. Click Browse Data and use the Table dropdown to switch between users, bills, settings.

From the Execute SQL tab you can run queries:
  SELECT id, username, role, active FROM users;
  SELECT id, guest, total, balance FROM bills ORDER BY created_at DESC;
  SELECT * FROM settings;

You'll see that salt and hash look like binary garbage. That is intentional and proves the passwords are not stored in plain text.

Do not edit salt or hash by hand — you'll lock yourself out. If you need to reset a password, use Admin > Manage users... > Reset password inside the app.

Troubleshooting
Problem                                                       | Fix
"No matching toolchains found" or a Java version error       | Install JDK 21: File > Project Structure > SDKs > + > Download JDK > version 21. Then Settings > Build Tools > Gradle > set Gradle JVM to 21
package ... does not match the expected package              | The file is in the wrong folder. Move it to the folder named in its package line
cannot find symbol for Logo, Theme, BillController...        | The file is missing or in the wrong folder. Compare with the layout above
incompatible types: LoginDialog cannot be converted to User  | SignUpDialog.java still contains the old showSignUp() returning LoginDialog. Replace it with the version returning User (see Accounts and access control)
class X is public, should be declared in a file named X.java | You pasted code for X into a file with a different name. Delete the file and create the correctly-named one
Tests are red or "cannot resolve org.junit"                  | Make sure build.gradle has the JUnit lines, then Gradle > Reload All Gradle Projects
Could not open the database: ... on startup                  | Delete lodge.db (see Where the data is saved) and relaunch — it will be recreated
database is locked in DB Browser                             | The Lodge app is still running. Close it, then reopen DB Browser
Logo does not show                                           | The file must be named logo.png and sit in src/main/resources. Rebuild and run again
Text looks too small or blurry on a high-resolution screen   | Run with the VM option -Dsun.java2d.uiScale=1.5 (Run > Edit Configurations > Modify options > Add VM options)
You forgot the officiating code                              | There is no recovery. Close the app, delete lodge.db, relaunch, and set a new one.

Ideas for later
- Edit a saved bill instead of deleting and re-entering it
- Move the price list from RoomType into a settings table so it can change without recompiling
- Encrypt the database file with SQLCipher (so lodge.db is unreadable even if copied to another machine)
- Add an audit log of who deactivated whom, and when
- Export the receipt as PDF directly instead of going through the print dialog
- Move to a client/server database (PostgreSQL) if multiple reception desks ever need to share data