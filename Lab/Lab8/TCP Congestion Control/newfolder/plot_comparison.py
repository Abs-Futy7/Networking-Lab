import matplotlib.pyplot as plt
import pandas as pd

# Read CSV data
tahoe_data = pd.read_csv('TAHOE_metrics.csv')
reno_data = pd.read_csv('RENO_metrics.csv')

# Create subplots
fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(15, 10))

# Plot 1: CWND vs Round
ax1.plot(tahoe_data['Round'], tahoe_data['CWND'], 'r-o', label='TCP Tahoe', markersize=4)
ax1.plot(reno_data['Round'], reno_data['CWND'], 'b-s', label='TCP Reno', markersize=4)
ax1.set_xlabel('Transmission Round')
ax1.set_ylabel('Congestion Window (CWND)')
ax1.set_title('CWND vs Transmission Round')
ax1.legend()
ax1.grid(True)

# Plot 2: SSThresh vs Round
ax2.plot(tahoe_data['Round'], tahoe_data['SSThresh'], 'r--', label='TCP Tahoe')
ax2.plot(reno_data['Round'], reno_data['SSThresh'], 'b--', label='TCP Reno')
ax2.set_xlabel('Transmission Round')
ax2.set_ylabel('Slow Start Threshold')
ax2.set_title('SSThresh vs Transmission Round')
ax2.legend()
ax2.grid(True)

# Plot 3: RTT vs Round
ax3.plot(tahoe_data['Round'], tahoe_data['RTT'], 'r-', label='TCP Tahoe')
ax3.plot(reno_data['Round'], reno_data['RTT'], 'b-', label='TCP Reno')
ax3.set_xlabel('Transmission Round')
ax3.set_ylabel('Round Trip Time (ms)')
ax3.set_title('RTT vs Transmission Round')
ax3.legend()
ax3.grid(True)

# Plot 4: Packet Loss Events
tahoe_loss_rounds = tahoe_data[tahoe_data['PacketLoss'] == 1]['Round']
reno_loss_rounds = reno_data[reno_data['PacketLoss'] == 1]['Round']
ax4.scatter(tahoe_loss_rounds, [1]*len(tahoe_loss_rounds), color='red', s=50, label='TCP Tahoe Loss')
ax4.scatter(reno_loss_rounds, [0.5]*len(reno_loss_rounds), color='blue', s=50, label='TCP Reno Loss')
ax4.set_xlabel('Transmission Round')
ax4.set_ylabel('Packet Loss Events')
ax4.set_title('Packet Loss Events Timeline')
ax4.legend()
ax4.grid(True)

plt.tight_layout()
plt.savefig('tcp_comparison.png', dpi=300, bbox_inches='tight')
plt.show()

print('Graphs saved as tcp_comparison.png')
