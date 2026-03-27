# Crypto1010 Developer Guide

## Acknowledgements
- Java Platform, Standard Edition 17 API documentation: https://docs.oracle.com/en/java/javase/17/docs/api/
- JUnit 5 User Guide (for unit testing approach and assertions): https://junit.org/junit5/docs/current/user-guide/
- Gradle User Manual (build and task orchestration): https://docs.gradle.org/current/userguide/userguide.html
- SHA-256 usage through Java `MessageDigest` API from the Java standard library documentation.

## Design & implementation
Crypto1010 is implemented as a modular command-line application with clear separation between input parsing, command execution, domain model, and persistence.

### High-level structure
- `Crypto1010` manages the main loop, input capture, and save/load lifecycle.
- `Parser` maps raw user input to concrete command objects.
- `command` package implements user-facing functionality (`create`, `send`, `balance`, etc.).
- `model` package contains core blockchain and wallet logic.
- `storage` package persists the blockchain to JSON (`data/blockchain.json`).

### Command execution flow
1. User enters command text in the CLI.
2. `Parser` extracts command word and arguments.
3. A concrete `Command` subclass is instantiated.
4. `Command.execute(...)` mutates/queries model state.
5. `Crypto1010` saves blockchain state after successful command execution.

### Blockchain model
- A `Blockchain` stores an ordered list of `Block`.
- Each `Block` has:
  - index
  - timestamp
  - previous hash
  - transactions
  - current hash (SHA-256 of block payload)
- `validate()` verifies:
  - hash consistency
  - previous-hash linkage
  - transaction data quality

### Blockchain Simulation Mode
- Simulation Mode allows developers and users to interact with a fully functional blockchain environment locally without connecting to a real network. 
- Test blockchain logic without real funds or network latency
- Debug transaction flows and consensus mechanisms 
- Demonstrate how blockchain systems work (educational use)
- Rapid prototyping of new features (smart contracts, validation rules, etc.)

### Transaction and balance logic
Transactions are represented in this format:
`sender -> receiver : amount`

Balance for a wallet is computed by scanning all transactions:
- subtract amount when wallet is sender
- add amount when wallet is receiver

### `send` command implementation
`SendCommand` uses prefix-based argument parsing:
- required: `w/`, `to/`, `amt/`
- optional: `speed/`, `fee/`, `note/`

Validation sequence:
1. parse prefixes
2. verify wallet exists
3. verify amount > 0
4. validate recipient address format
5. resolve fee (manual or speed-based)
6. verify sufficient balance (`amount + fee`)
7. append transfer transaction and optional network-fee transaction

### Persistence implementation
- `BlockchainStorage` serializes blockchain state to JSON.
- On startup, `Crypto1010` attempts to load and validate persisted data.
- If loading fails, a default blockchain is initialized.
- Wallets are currently in-memory only and are not persisted.

### Suggested UML diagrams (update this plz)
- Class diagram: `Command` hierarchy, `Parser`, `Crypto1010`, model classes.
- Sequence diagram: end-to-end execution of `send`.
- Sequence diagram: validation flow in `validate`.

## Product scope

### Target user profile
- Students and beginners learning blockchain fundamentals through a terminal-based workflow.
- Users who prefer lightweight, text-based interaction instead of a GUI.
- Developers who want a small Java codebase suitable for extension and experimentation.

### Value proposition
Crypto1010 provides a compact, practical environment to understand wallet transfers, block structure, hash linkage, and blockchain validation without requiring external infrastructure or a real network.

## User Stories

| Version | As a ... | I want to ... | So that I can ... |
|--------|----------|---------------|------------------|
| v1.0 | new user | view usage instructions | quickly learn available commands |
| v1.0 | user | create wallets | simulate distinct senders and receivers |
| v1.0 | user | list wallets | confirm available wallets in the current session |
| v1.0 | user | check wallet balance | verify transaction effects numerically |
| v1.0 | user | send funds with fee controls | model transfer and fee trade-offs |
| v1.0 | user | validate the blockchain | confirm chain integrity after modifications |
| v1.0 | user | inspect a specific block | view exact block-level transaction data |

### Planned enhancement: account switching
- User story: As a user, I can switch accounts and save progress.
- Add account switching.
- Load/save different wallet states.
- Improve persistence logic.

## Non-Functional Requirements
- The application shall run on Java 17.
- The application shall be usable entirely via CLI input/output.
- Blockchain data shall persist locally in `data/blockchain.json`.
- Validation shall be deterministic for the same stored blockchain input.
- The codebase shall remain modular enough to support adding new commands with minimal cross-component changes.
- The project shall support automated unit testing via JUnit 5 and Gradle.

## Glossary
- Blockchain: Ordered chain of blocks linked by previous hashes.
- Block: A unit containing index, timestamp, previous hash, current hash, and transactions.
- Wallet: A logical identity used as sender/receiver in transactions.
- Transaction: A transfer record in the format `sender -> receiver : amount`.
- Validation: Integrity checks covering hash correctness, linkage, and transaction validity.
- Network fee account: Internal sink (`network-fee`) receiving fee deductions from `send`.

## Instructions for manual testing

### Prerequisites
- Java 17 installed and configured.
- Project cloned locally.

### Running the app
1. Run `./gradlew run` (or `.\gradlew run` on Windows PowerShell).

### Manual test cases
1. Create wallets:
   - `create alice`
   - `create bob`
   - Expected: confirmation messages for each wallet.
1. List wallets:
   - `list`
   - Expected: numbered wallet list including `alice` and `bob`.
1. Check balance:
   - `balance w/bob`
   - Expected: balance displayed with 8 decimal places.
1. Successful transfer:
   - `send w/bob to/0x1111111111111111111111111111111111111111 amt/1`
   - Expected: success output including wallet, recipient, amount, speed, and fee.
1. Invalid transfer format:
   - `send invalid`
   - Expected: invalid send format error.
1. Invalid recipient address:
   - `send w/bob to/not-an-address amt/1`
   - Expected: invalid recipient address error.
1. Validate chain:
   - `validate`
   - Expected: valid-chain success message unless data is corrupted.
1. View block details:
   - `viewblock 1`
   - Expected: full block metadata and transaction list.
1. Out-of-range block:
   - `viewblock 999`
   - Expected: block index out of range error.
1. Exit:
   - `exit`
   - Expected: program terminates and blockchain state is saved.

### Data reset / test isolation
- Delete or replace `data/blockchain.json` to reset blockchain state between manual test runs.
