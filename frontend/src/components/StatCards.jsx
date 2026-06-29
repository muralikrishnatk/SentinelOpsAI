export default function StatCards({ stats }) {
  if (!stats) return null;
  const cards = [
    { k: 'Total Incidents', v: stats.total },
    { k: 'Open', v: stats.open },
    { k: 'Resolved', v: stats.resolved },
    { k: 'MTTR (min)', v: stats.meanTimeToResolveMinutes },
  ];
  return (
    <>
      {cards.map((c) => (
        <div className="card" key={c.k}>
          <div className="k">{c.k}</div>
          <div className="v">{c.v}</div>
        </div>
      ))}
    </>
  );
}
