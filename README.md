# Pharmacy-Sys

A multithreaded Java client-server desktop application simulating a real-world pharmacy supply chain. Multiple pharmacy branches (clients) connect in real time to a central warehouse (server) to browse medicine inventory, build orders in a cart, and receive itemized receipts.

## Features

- **Real-time multi-client support** — Java multithreading, one thread per connected pharmacy branch
- **Shopping cart** — add, merge, and remove items before checkout
- **Prescription enforcement** — Rx medicines require the prescription checkbox to be checked
- **Automatic discounts** — 15% on prescription drugs, 5% on over-the-counter medicines
- **Persistent inventory** — stock levels update and save to CSV after every order
- **Itemized receipts** — per-line discount breakdown and grand total

## Architecture

Client-server architecture over TCP sockets. The server is a multithreaded warehouse; each client is a Swing GUI acting as a pharmacy branch.

```
                    ┌─────────────────────┐
                    │   PharmacyServer     │
                    │  (ServerSocket:5000) │
                    └──────────┬───────────┘
                               │ spawns one thread per client
                    ┌──────────▼───────────┐
                    │    ClientHandler      │
                    │ validates stock & Rx  │
                    │ updates inventory.csv │
                    └──────────┬───────────┘
                               │ ObjectOutputStream
              ┌────────────────┼────────────────┐
    ┌─────────▼──────────┐         ┌────────────▼─────────┐
    │  PharmacyClientGUI  │   ...   │  PharmacyClientGUI    │
    │  (Swing, branch 1)  │         │  (Swing, branch N)    │
    └──────────────────────┘         └────────────────────┘
```

**Connection flow:**
1. `PharmacyServer` opens a `ServerSocket` on port 5000.
2. Each incoming connection spawns a new `ClientHandler` thread.
3. The handler sends the full inventory to the client via `ObjectOutputStream`.
4. The client submits order requests as `[medicineName, quantity, hasPrescription]`.
5. The server validates stock and prescription requirements, returning `SUCCESS`/`ERROR` per item.
6. The client renders the final itemized receipt.

Socket I/O on the client runs inside a `SwingWorker` background thread so the GUI never freezes while waiting on the server.

## Project Structure

```
src/
├── model/
│   ├── Medicine.java              # abstract base class
│   ├── PrescriptionDrug.java      # 15% discount, requires prescription
│   └── OverTheCounter.java        # 5% discount, no prescription needed
├── util/
│   └── FileManager.java           # reads/writes inventory.csv
├── server/
│   ├── PharmacyServer.java        # accepts connections, spawns client threads
│   └── ClientHandler.java         # validates orders, updates inventory
└── gui/
    └── PharmacyClientGUI.java     # inventory table, cart, checkout, receipt
```

## Object-Oriented Design

- **Inheritance** — `PrescriptionDrug` and `OverTheCounter` extend the abstract `Medicine` class, inheriting shared fields/methods and overriding only what differs.
- **Polymorphism** — `getDiscountRate()` is abstract in `Medicine` and overridden per subclass; the server calls it on any `Medicine` reference and gets the correct rate automatically.
- **Encapsulation** — all `Medicine` fields are private, exposed only through public getters/setters.
- **Serialization** — `Medicine` implements `Serializable` so objects can be sent directly over sockets via `ObjectOutputStream`, without manual string conversion.

## Data Persistence

Inventory is stored in `inventory.csv` at the project root:

```
name,type,manufacturer,price,stock,extra
Amoxicillin 500mg,Rx,PharmaCo,25.00,100,ClassB
Paracetamol 500mg,OTC,HealthPlus,5.00,500,Painkiller
```

| Column | Meaning |
|---|---|
| `name` | Full medicine name |
| `type` | `Rx` (prescription) or `OTC` (over-the-counter) |
| `manufacturer` | Company name |
| `price` | Original price before discount |
| `stock` | Current units available |
| `extra` | Rx: prescription class (ClassA/B/C) · OTC: category (Painkiller, Vitamin, etc.) |

`FileManager.saveInventory()` runs inside a synchronized method on the server so concurrent client orders can't corrupt the file.

## Tech Stack

- Java
- Java Sockets (TCP, `ObjectOutputStream`/`ObjectInputStream`)
- Java Swing (`SwingWorker` for async GUI)
- CSV file persistence

## Getting Started

```bash
# Compile
javac -d bin src/model/*.java src/util/*.java src/server/*.java src/gui/*.java

# Run the server (start first)
java -cp bin server.PharmacyServer

# Run one or more clients
java -cp bin gui.PharmacyClientGUI
```

## Author

Belal Kandil
