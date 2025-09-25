Perfect, Levin — with your **'In scope'** column in Sheet3 indicating `"Yes"` or `"No"` instead of relying on cell color, we can now build a clean, formula-only solution. Here's how to do it:

---

## ✅ Goal

In **Sheet1**, for each row:
- Match **Product ID** and **Max Weight** with **Sheet2** to get the **Price ID**.
- Use **Price ID** and **Weight** to find the correct price range in **Sheet3**.
- Return the **Price** only if `'In scope' = "Yes"` in Sheet3.
- If **Price ID is missing** or **not in scope**, return `"NA"`.

---

## 🧮 Final Formula (Excel 365+ or Excel 2021, entered in Sheet1 row 2):

```excel
=LET(
  pid, A2,                      // Product ID from Sheet1
  wt, B2,                       // Max Weight from Sheet1
  row2, XMATCH(1, (Sheet2!A:A=pid)*(Sheet2!B:B=wt)), // Row in Sheet2
  priceID, INDEX(Sheet2!C:C, row2),                 // Price ID from Sheet2
  row3, XMATCH(1, (Sheet3!A:A=priceID)*(Sheet3!B:B<=wt)*(Sheet3!C:C>=wt)*(Sheet3!E:E="Yes")), // Row in Sheet3
  IF(ISBLANK(priceID), "NA", IF(ISNUMBER(row3), INDEX(Sheet3!D:D, row3), "NA"))
)
```

---

## 🧩 Column Mapping

| Sheet | Column | Description |
|-------|--------|-------------|
| Sheet1 | A      | Product ID |
| Sheet1 | B      | Max Weight |
| Sheet2 | A      | Product ID |
| Sheet2 | B      | Max Weight |
| Sheet2 | C      | Price ID |
| Sheet3 | A      | Price ID |
| Sheet3 | B      | Weight From |
| Sheet3 | C      | Weight To |
| Sheet3 | D      | Price |
| Sheet3 | E      | In Scope ("Yes"/"No") |

---

## 🧠 How It Works

- `XMATCH(1, ...)` finds the row where conditions match.
- `INDEX(..., row)` retrieves the corresponding value.
- `LET(...)` simplifies and speeds up the formula by storing intermediate values.
- `IF(ISBLANK(priceID), "NA", ...)` handles missing Price IDs.
- `IF(ISNUMBER(row3), ...)` ensures the price is returned only if a valid match is found **and** `'In scope' = "Yes"`.

---

## 🧪 Compatibility Notes

- This formula uses `LET` and `XMATCH`, available in **Excel 365 and Excel 2021+**.
- If you're using an older version, I can rewrite it using `INDEX` and `MATCH` instead — just let me know.

Would you like me to help turn this into a reusable named formula or apply it across a full table with dynamic ranges?


Yes, you can absolutely place the formula in **Column C of Sheet1** to calculate the price for each Product ID row — but the error you're seeing is likely due to one of these common issues:

---

### ⚠️ Common Causes of “There's a problem with this formula” Error

1. **Array formula syntax in older Excel versions**  
   If you're not using **Excel 365 or Excel 2021**, functions like `LET` and `XMATCH` may not be supported. In that case, you’ll need a version using `INDEX` and `MATCH`.

2. **Missing implicit multiplication operator (`*`) in array logic**  
   Excel formulas using multiple conditions like `(A:A=...) * (B:B=...)` must be entered as **array formulas** in older Excel.

3. **Full column references (`A:A`) in array formulas**  
   These can cause performance issues or errors. It's better to use limited ranges like `A2:A100`.

4. **Formula not entered with Ctrl+Shift+Enter**  
   In older Excel, array formulas must be confirmed with **Ctrl+Shift+Enter**, not just Enter.

---

### ✅ Compatible Formula (for Column C in Sheet1)

Here’s a version using `INDEX` and `MATCH` that works in **all Excel versions**, assuming your data is in rows 2 to 100:

```excel
=IFERROR(
  IF(
    ISBLANK(
      INDEX(Sheet2!C$2:C$100, MATCH(1, (Sheet2!A$2:A$100=A2)*(Sheet2!B$2:B$100=B2), 0))
    ),
    "NA",
    IF(
      INDEX(Sheet3!E$2:E$100,
        MATCH(1,
          (Sheet3!A$2:A$100=INDEX(Sheet2!C$2:C$100, MATCH(1, (Sheet2!A$2:A$100=A2)*(Sheet2!B$2:B$100=B2), 0))) *
          (Sheet3!B$2:B$100<=B2) *
          (Sheet3!C$2:C$100>=B2),
        0)
      )="Yes",
      INDEX(Sheet3!D$2:D$100,
        MATCH(1,
          (Sheet3!A$2:A$100=INDEX(Sheet2!C$2:C$100, MATCH(1, (Sheet2!A$2:A$100=A2)*(Sheet2!B$2:B$100=B2), 0))) *
          (Sheet3!B$2:B$100<=B2) *
          (Sheet3!C$2:C$100>=B2),
        0)
      ),
      "NA"
    )
  ),
"NA")
```

> ⚠️ **Important**: This is an **array formula**. In Excel 2016 or earlier, press **Ctrl+Shift+Enter** after pasting it into cell `C2`. In Excel 365/2021+, just press Enter.

---

### 🧩 What It Does

- Matches `Product ID` and `Max Weight` in Sheet2 to get the `Price ID`.
- Uses `Price ID` and `Weight` to find the correct row in Sheet3.
- Checks if `'In scope' = "Yes"` in Sheet3.
- Returns the price or `"NA"`.

---

### 🪄 Apply to All Rows

Once you’ve confirmed the formula works in `C2`, drag it down to fill all rows in Column C where `Product ID` exists.

Would you like a version using named ranges or a dynamic table setup for easier maintenance?

