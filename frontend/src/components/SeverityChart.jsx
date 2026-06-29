import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';

const COLORS = {
  SEV1: '#ff5c5c',
  SEV2: '#ff9f43',
  SEV3: '#ffd54a',
  SEV4: '#5cc8ff',
};

export default function SeverityChart({ stats }) {
  if (!stats) return null;
  const data = Object.entries(stats.bySeverity || {}).map(([name, value]) => ({
    name,
    value,
  }));

  if (data.length === 0) {
    return <div className="panel muted">No incidents yet.</div>;
  }

  return (
    <div className="panel" style={{ height: 240 }}>
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            nameKey="name"
            innerRadius={48}
            outerRadius={80}
            paddingAngle={3}
          >
            {data.map((entry) => (
              <Cell key={entry.name} fill={COLORS[entry.name] || '#888'} />
            ))}
          </Pie>
          <Tooltip
            contentStyle={{
              background: '#1b2230',
              border: '1px solid #232c3d',
              borderRadius: 8,
              color: '#e6edf3',
            }}
          />
          <Legend wrapperStyle={{ fontSize: 12, color: '#8b97a8' }} />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}
