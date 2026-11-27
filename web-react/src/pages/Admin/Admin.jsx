import { Fragment, useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { apiGet } from "../../api/client";
import { useUser } from "../../context/UserContext";
import "./Admin.css";

/**
 * Admin , the back office.
 *
 * Deliberately NOT in the sticky-note vocabulary the rest of the app uses.
 * The notes are the user's training journal, where the content is personal and
 * glanceable. This page is a ledger: counts with denominators, read by scanning
 * down a column of aligned figures. Notes would break that alignment once per
 * note and cap how many rows fit before one needs its own scrollbar.
 *
 * Strings are inline English rather than translation keys. There is exactly one
 * reader, and routing these through i18n would push a back-office vocabulary to
 * the translators along with the product.
 */

const PCT = (part, whole) => (whole ? Math.round((part / whole) * 100) : 0);

/** A line, scaled to its own range. Nothing is drawn below two points. */
function Spark({ values, label }) {
  if (!values || values.length < 2) return null;

  const max = Math.max(...values);
  const min = Math.min(...values);
  const span = max - min || 1;
  const step = 600 / (values.length - 1);

  const points = values
    .map((v, i) => `${(i * step).toFixed(1)},${(38 - ((v - min) / span) * 34).toFixed(1)}`)
    .join(" ");

  return (
    <svg className="adm-spark" viewBox="0 0 600 42" preserveAspectRatio="none" role="img"
         aria-label={`${label}, ${min} to ${max} over ${values.length} days`}>
      <polyline points={points} fill="none" stroke="currentColor" strokeWidth="2"
                strokeLinecap="round" strokeLinejoin="round" vectorEffect="non-scaling-stroke" />
    </svg>
  );
}

/** One ledger line: what it counts, the figure, and the figure's context. */
function Row({ label, value, note }) {
  return (
    <div className="adm-row">
      <span className="adm-row-label">{label}</span>
      <span className="adm-row-value">{value}</span>
      <span className="adm-row-note">{note}</span>
    </div>
  );
}

function Section({ title, hint, children }) {
  return (
    <section className="adm-section">
      <h2 className="adm-section-title">{title}</h2>
      {hint && <p className="adm-hint">{hint}</p>}
      {children}
    </section>
  );
}

export function Admin() {
  const { user, loading: userLoading } = useUser();
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    apiGet("/admin/stats")
      .then((res) => {
        if (!res.ok) throw new Error(String(res.status));
        return res.json();
      })
      .then(setData)
      .catch(() => setError("Could not load the figures."));
  }, []);

  // Convenience only. The 403 on the endpoint is the real boundary, and this
  // repository is public, so the route is readable in main.py either way.
  if (userLoading) return null;
  if (user && !user.is_admin) return <Navigate to="/dashboard" replace />;

  if (error) return <p className="adm-empty">{error}</p>;
  if (!data) return <p className="adm-empty">Loading.</p>;

  const { today, history, cohorts } = data;

  const sources = Object.entries(today.by_source?.registered || {})
    .sort((a, b) => b[1] - a[1]);
  const sourceMax = sources.length ? sources[0][1] : 0;

  // Only weeks some cohort has actually reached get a column.
  const weekCount = cohorts.reduce((n, c) => Math.max(n, c.weeks.length), 0);
  const weeks = Array.from({ length: weekCount }, (_, i) => i);

  const asOf = new Date().toISOString().slice(0, 16).replace("T", ", ");

  return (
    <div className="adm">
      <h1 className="adm-title">Admin</h1>
      <p className="adm-meta">
        {`As at ${asOf} UTC. ${history.length} ${history.length === 1 ? "day" : "days"} of snapshots.`}
      </p>

      <Section title="People">
        <Row label="Registered" value={today.registered}
             note={`+${today.new_signups} today`} />
        <Row label="Verified their email" value={today.verified}
             note={`${PCT(today.verified, today.registered)}%`} />
        <Row label="Built a routine" value={today.with_routine}
             note={`${PCT(today.with_routine, today.registered)}%`} />
        <Row label="Logged a workout" value={today.activated}
             note={`${PCT(today.activated, today.registered)}%`} />
      </Section>

      <Section title="Activity"
               hint="Counted from when a workout was logged, not the date it was logged for.">
        <Row label="Logged today" value={today.dau} note={`of ${today.activated}`} />
        <Row label="Logged this week" value={today.wau} note={`of ${today.activated}`} />
        <Row label="Logged this month" value={today.mau} note={`of ${today.activated}`} />
        <Row label="Workouts recorded today" value={today.sessions_logged} note="" />
      </Section>

      <Section title="Over time">
        {history.length < 2 ? (
          <p className="adm-hint">Two days of snapshots are needed before a line means anything.</p>
        ) : (
          <div className="adm-trends">
            <span className="adm-trend-label">Registered</span>
            <Spark label="Registered" values={history.map((r) => r.registered)} />
            <span className="adm-trend-label">Logged this week</span>
            <Spark label="Logged this week" values={history.map((r) => r.wau)} />
          </div>
        )}
      </Section>

      <Section title="Where they signed up"
               hint="Unknown is every account created before the client header shipped. It only shrinks.">
        {!sources.length && <p className="adm-hint">No accounts yet.</p>}
        <div className="adm-bars">
          {sources.map(([name, count]) => (
            <Fragment key={name}>
              <span className="adm-bar-label">{name}</span>
              <div className="adm-track">
                <div className={`adm-fill${name === "unknown" ? " is-unknown" : ""}`}
                     style={{ width: `${PCT(count, sourceMax)}%` }} />
              </div>
              <span className="adm-bar-value">{count}</span>
            </Fragment>
          ))}
        </div>
      </Section>

      <Section title="Who came back"
               hint="Of everyone who signed up in one week, how many logged a workout in each week after. The denominator stays at the cohort's size. Week 0 is the signup week itself, so it is activation rather than retention.">
        {!cohorts.length ? (
          <p className="adm-hint">No signups in the last twelve weeks.</p>
        ) : (
          <table className="adm-cohorts">
            <thead>
              <tr>
                <th scope="col">Signed up</th>
                <th scope="col" className="adm-num">Size</th>
                {weeks.map((w) => (
                  <th scope="col" className="adm-num" key={w}>{`Wk ${w}`}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {cohorts.map((c) => (
                <tr key={c.cohort_start}>
                  <th scope="row">{c.cohort_start}</th>
                  <td className="adm-num">{c.size}</td>
                  {weeks.map((w) => (
                    <td className="adm-num" key={w}
                        title={w < c.weeks.length ? `${PCT(c.weeks[w], c.size)}% of ${c.size}` : "Not yet"}>
                      {w < c.weeks.length
                        ? c.weeks[w]
                        : <span className="adm-nyet" aria-label="Week has not elapsed">&middot;</span>}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Section>
    </div>
  );
}
