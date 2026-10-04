# Card Reader Troubleshooting

## Card not reading
1. Ask the customer to tap instead of insert (or insert instead of tap).
2. Check the chip end is inserted fully and left in until the reader beeps.
3. Wipe the reader slot with a dry cloth.
4. If three cards in a row fail, restart the reader: hold the power button for 10 seconds.

## Reader offline
- Check the reader's USB/Ethernet cable and that the status light is solid green.
- Card payments cannot be taken while the reader is offline; take cash or move the customer to another till.

## Customer charged twice?
The POS sends each checkout with a unique idempotency key, so a retried checkout cannot create a second charge. What
the customer usually sees is a pending authorization from a declined or abandoned attempt; it drops off their
statement within 1-3 business days. Look the transaction up to confirm there is only one COMPLETED sale.
