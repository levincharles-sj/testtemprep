To incorporate the condition that **only yellow-highlighted Price IDs in Sheet3 are valid**, you'll need to use a **helper column** in Sheet3 to flag those yellow cells, because Excel formulas **cannot directly detect cell color**.

Here’s how to do it:

---

### 🛠 Step 1: Add a Helper Column in Sheet3

In a new column (say, Column E), use VBA to mark yellow-highlighted cells. Here's a simple macro:

```vba
Sub FlagYellowPriceIDs()
    Dim cell As Range
    For Each cell In Sheets("Sheet3").Range("A2:A100") ' Adjust range as needed
        If cell.Interior.Color = RGB(255, 255, 0) Then
            cell.Offset(0, 4).Value = "Valid"
        Else
            cell.Offset(0, 4).Value = "Invalid"
        End If
    Next cell
End Sub
```

This will label each Price ID row in Sheet3 as `"Valid"` or `"Invalid"` in Column E.

---

### 🧮 Step 2: Update Your Formula in Sheet1

Now modify the formula to check for `"Valid"` in Sheet3’s helper column:

```excel
=IFERROR(
  IF(
    ISBLANK(
      INDEX(Sheet2!C:C, MATCH(1, (Sheet2!A:A=A2)*(Sheet2!B:B=B2), 0))
    ),
    "NA",
    IF(
      INDEX(
        Sheet3!E:E,
        MATCH(
          1,
          (Sheet3!A:A=INDEX(Sheet2!C:C, MATCH(1, (Sheet2!A:A=A2)*(Sheet2!B:B=B2), 0))) *
          (Sheet3!B:B<=B2) *
          (Sheet3!C:C>=B2),
          0
        )
      )="Valid",
      INDEX(
        Sheet3!D:D,
        MATCH(
          1,
          (Sheet3!A:A=INDEX(Sheet2!C:C, MATCH(1, (Sheet2!A:A=A2)*(Sheet2!B:B=B2), 0))) *
          (Sheet3!B:B<=B2) *
          (Sheet3!C:C>=B2),
          0
        )
      ),
      "NA"
    )
  ),
"NA")
```

---

### ✅ What This Does

- Gets the Price ID from Sheet2.
- Checks if that Price ID exists in Sheet3 **and** is marked `"Valid"` (i.e., yellow-highlighted).
- Matches the weight range.
- Returns the price if valid, otherwise `"NA"`.

---

Would you like me to help you turn this into a reusable template or add conditional formatting to visualize the results?
