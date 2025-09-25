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
