<div align="center">
    <img src="./gauction.gif" alt="gAuction" width="300" height="300" style="margin-right: 40px;">
    <br clear="left"/>
    <div style="text-align: left; max-width: 600px;">
        <p>
            A robust and intuitive auction system for Minecraft servers, offering a complete trading experience through a modern and user-friendly graphical interface.
        </p>
        <p>
            With advanced features such as proportional increment, auction queue, detailed history, and warehouse system, gAuction provides a complete solution for servers seeking a reliable and easy-to-use auction system.
        </p>
        <p>
            Developed with both players and administrators in mind, it offers granular controls, permission system, and a variety of commands for efficient management.
        </p>
    </div>
</div>

<br clear="left"/>

## 📋 Features

- **Real-Time Auction System**
  - Auctions with automatic timer and warnings
  - Incremental bidding system (fixed or percentage)
  - Organized auction queue
  - Complete transaction history
  - Item warehouse system

- **Intuitive Graphical Interface**
  - Main menu with all functionalities
  - Quick and custom bidding system
  - Auction queue visualization and management
  - Detailed history with personal filters
  - Item collection warehouse

- **Administrative Features**
  - Complete control over active auctions
  - Item banning system (hand item or chest)
  - Flexible duration configuration
  - Queue management
  - Administrative commands

- **Integrated Economy**
  - Full Vault support
  - Configurable publication fees
  - Automatic refund system
  - Detailed financial history
  - Configurable bid increments

## 🚀 Installation

1. Make sure you have Vault installed
2. Download the latest `.jar` file [here](https://github.com/g-soldera/gAuction/releases)
3. Place the file in your server's `plugins` folder
4. Restart the server
5. Configure the plugin as needed

### 📋 Dependencies

- Vault
- Minecraft Server 1.20+
- Java 17 or higher

## ⚙️ Configuration

### config.yml

```yaml
# Auction settings
auction:
  duration: 300 # Default auction duration in seconds
  max_queue_size: 10 # Maximum auction queue size
  step:
    enabled: true # Enable minimum increment
    percentage: 10.0 # Percentage of initial value
  fees:
    publication: 0.0 # Fee for creating an auction
    bid: 0.0 # Fee on final bid
  banned_items: [] # List of banned items

# Database settings
database:
  type: SQLITE # Database type (SQLITE, MYSQL)
  host: localhost # Database host (MySQL)
  port: 3306 # Database port (MySQL)
  name: gauction # Database name (MySQL)
  user: root # Database user (MySQL)
  password: "" # Database password (MySQL)
```

## 🎮 Commands

### Player Commands
- `/auction` - Opens the main menu
- `/auction create [min_bid] [increment]` - Creates a new auction
- `/auction bid <amount>` - Places a bid on current auction
- `/auction info` - Shows current auction information

### Admin Commands
- `/auctionadmin banitem` - Bans item in hand
- `/auctionadmin banchest` - Bans items in chest
- `/auctionadmin setduration <seconds>` - Sets auction duration
- `/auctionadmin cancelauction` - Cancels current auction
- `/auctionadmin forcestart [min bid] [increment]` - Force starts an auction
- `/auctionadmin reload` - Reloads configuration

## 🔒 Permissions

- `gauction.admin` - Access to administrative commands

## 📦 Detailed Features

### Auction System
- Creation via command or interface
- Configurable automatic increment
- Timer with warnings
- Organized queue system
- Cancellation and management

### Graphical Interface
- Intuitive main menu
- Simplified bidding system
- Auction queue visualization
- History with filters
- Item warehouse

### Warehouse System
- Automatic item storage
- Individual or mass collection
- Detailed item status
- Secure collection system

## 📝 License

This project is under the MIT license - see the [LICENSE.md](LICENSE.md) file for details

## 👥 Author

- **Gustavo Soldera** - *Development* - [gsoldera](https://github.com/g-soldera)

## 📞 Support

For support, open an issue on GitHub or contact us through Discord