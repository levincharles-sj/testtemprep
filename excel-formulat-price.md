To achieve this in Excel, you’ll need a formula that:

1. **Matches Product ID and Max Weight from Sheet1 to Sheet2** to get the corresponding Price ID.
2. **Uses the Price ID and Weight from Sheet1** to find the correct price range in Sheet3.
3. **Returns the price from Sheet3**, or `"NA"` if Price ID is missing.

Here’s a step-by-step formula approach using `INDEX`, `MATCH`, and `IFERROR`:

---

### 🧮 Final Formula (to be placed in Sheet1, assuming row 2):

```excel
=IFERROR(
  IF(
    ISBLANK(
      INDEX(Sheet2!C:C, MATCH(1, (Sheet2!A:A=A2)*(Sheet2!B:B=B2), 0))
    ),
    "NA",
    INDEX(
      Sheet3!D:D,
      MATCH(
        1,
        (Sheet3!A:A=INDEX(Sheet2!C:C, MATCH(1, (Sheet2!A:A=A2)*(Sheet2!B:B=B2), 0))) *
        (Sheet3!B:B<=B2) *
        (Sheet3!C:C>=B2),
        0
      )
    )
  ),
"NA")
```

---

### 🧩 Explanation

Assuming:
- `Sheet1!A2` = Product ID
- `Sheet1!B2` = Max Weight
- `Sheet2!A:A` = Product ID
- `Sheet2!B:B` = Max Weight
- `Sheet2!C:C` = Price ID
- `Sheet3!A:A` = Price ID
- `Sheet3!B:B` = Weight From
- `Sheet3!C:C` = Weight To
- `Sheet3!D:D` = Price

**Steps:**
1. `MATCH(1, (Sheet2!A:A=A2)*(Sheet2!B:B=B2), 0)` finds the row in Sheet2 matching Product ID and Max Weight.
2. `INDEX(Sheet2!C:C, ...)` gets the Price ID.
3. If Price ID is blank, return `"NA"`.
4. Otherwise, match that Price ID in Sheet3 and check if Sheet1’s weight falls within the `Weight From` and `Weight To` range.
5. Return the corresponding price from Sheet3.

---

### ⚠️ Notes

- This formula is an **array formula**. In older versions of Excel, press **Ctrl+Shift+Enter**. In Excel 365 or Excel 2021+, just press Enter.
- Ensure all ranges are consistent and don’t contain headers in the referenced rows.
- You can optimize performance by limiting ranges (e.g., `A2:A100` instead of `A:A`).

Would you like a version using named ranges or helper columns for clarity and maintainability?
