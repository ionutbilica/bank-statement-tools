# Import Bank Statement Tools
Tools for normalizing csv bank statements.

## ING
ING Romania csv bank statement is not a csv file. One transaction spans over multiple lines, with no consistent columns.

Run the normalizing tool with:
```bash
normalize-ing.bat c:/download/ing-statements
``` 
It will output a csv file (target/ing.csv) with the normalized content of all csv files found in the given directory, with the following header:

|Date|Account|Debit|Credit|Details|
|----|-------|-----|------|-------|

The Date values are formatted as DD-MM-YYYY.

The Debit (money spent) and Credit (money received) are formatted as 1.234.567,23.

The Account values are all hardcoded to "ing".

Original intent is to normalize data to be able to import them into [Finance41.com](https://finance41.com).

Feel free to fork and tailor to your needs. Feedback at ionut.bilica@gmail.com. Code review appreciated.

Revolut statement normalizing coming soon. 

