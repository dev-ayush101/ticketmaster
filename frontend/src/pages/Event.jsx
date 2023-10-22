import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import api from '../api';

export default function Event() {
  const { eventId } = useParams();
  const navigate = useNavigate();
  const [event, setEvent] = useState(null);
  const [tickets, setTickets] = useState([]);
  const [selected, setSelected] = useState([]);
  const [loading, setLoading] = useState(true);
  const [booking, setBooking] = useState(false);

  // queue state
  const [email, setEmail] = useState('');
  const [inQueue, setInQueue] = useState(false);
  const [admitted, setAdmitted] = useState(false);
  const [position, setPosition] = useState(0);

  const joinQueue = async () => {
    if (!email.trim()) return toast.error('Enter your email');
    try {
      const res = await api.post(`/queue/${eventId}/join?userEmail=${encodeURIComponent(email)}`);
      setPosition(res.data.position);
      if (res.data.admitted) {
        setAdmitted(true);
      } else {
        setInQueue(true);
      }
    } catch (err) {
      toast.error('Failed to join queue');
    }
  };

  // SSE for queue position updates
  useEffect(() => {
    if (!inQueue) return;

    const source = new EventSource(`/api/queue/${eventId}/stream?userEmail=${encodeURIComponent(email)}`);

    source.addEventListener('queue-update', (e) => {
      const data = JSON.parse(e.data);
      setPosition(data.position);
      if (data.admitted) {
        setAdmitted(true);
        setInQueue(false);
        source.close();
      }
    });

    return () => source.close();
  }, [inQueue, eventId, email]);

  // load event + seat SSE once admitted
  useEffect(() => {
    if (!admitted) return;

    api.get(`/events/${eventId}`).then(res => {
      setEvent(res.data.event);
      setTickets(res.data.tickets);
      setLoading(false);
    });

    const eventSource = new EventSource(`/api/events/${eventId}/stream`);

    eventSource.addEventListener('seat-update', (e) => {
      const update = JSON.parse(e.data);
      setTickets(prev => prev.map(t =>
        t.id === update.ticketId ? { ...t, status: update.status } : t
      ));
      setSelected(prev => prev.filter(id => id !== update.ticketId));
    });

    return () => eventSource.close();
  }, [admitted, eventId]);

  const toggleSeat = (ticket) => {
    if (ticket.status !== 'AVAILABLE') return;
    setSelected(prev =>
      prev.includes(ticket.id)
        ? prev.filter(id => id !== ticket.id)
        : [...prev, ticket.id]
    );
  };

  const handleReserve = async () => {
    if (selected.length === 0) return toast.error('Select at least one seat');
    setBooking(true);
    try {
      const res = await api.post(`/bookings/${eventId}`, {
        ticketIds: selected,
        userEmail: email
      });
      toast.success('Seats reserved!');
      navigate(`/checkout/${res.data.id}?email=${encodeURIComponent(email)}`);
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to reserve');
      setBooking(false);
    }
  };

  const getSeatColor = (ticket) => {
    if (selected.includes(ticket.id)) return 'bg-[#1ea83c] text-white';
    if (ticket.status === 'BOOKED') return 'bg-gray-300 text-gray-400 cursor-not-allowed';
    if (ticket.status === 'RESERVED') return 'bg-[#f5d76e] text-gray-700 cursor-not-allowed';
    return 'bg-white border-2 border-[#1ea83c] text-[#1ea83c] hover:bg-[#e8f5e9] cursor-pointer';
  };

  // --- WAITING ROOM ---
  if (!admitted) {
    return (
      <div className="max-w-md mx-auto px-6 py-20 text-center">
        <h1 className="text-2xl font-bold text-gray-900 mb-2">Waiting Room</h1>

        {!inQueue ? (
          <>
            <p className="text-gray-500 text-sm mb-6">Enter your email to join the queue for this event.</p>
            <input
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && joinQueue()}
              placeholder="you@example.com"
              className="w-full px-4 py-2.5 border border-gray-300 rounded mb-4 text-sm focus:outline-none focus:border-[#dc3558]"
            />
            <button
              onClick={joinQueue}
              className="w-full py-2.5 bg-[#dc3558] text-white rounded font-medium hover:bg-[#c22d4e] text-sm"
            >
              Join Queue
            </button>
          </>
        ) : (
          <>
            <div className="mt-8 mb-4">
              <p className="text-6xl font-bold text-[#dc3558]">{position}</p>
              <p className="text-gray-500 text-sm mt-2">Your position in line</p>
            </div>
            <p className="text-gray-400 text-xs">Hang tight — you'll be admitted automatically.</p>
            <div className="mt-6 flex justify-center">
              <div className="w-6 h-6 border-2 border-[#dc3558] border-t-transparent rounded-full animate-spin" />
            </div>
          </>
        )}
      </div>
    );
  }

  // --- SEAT MAP (only after admitted) ---
  if (loading) return <p className="p-8 text-gray-500">Loading...</p>;

  const rows = {};
  tickets.forEach(t => {
    if (!rows[t.rowName]) rows[t.rowName] = [];
    rows[t.rowName].push(t);
  });
  Object.values(rows).forEach(r => r.sort((a, b) => a.seatNumber - b.seatNumber));
  const sortedRowNames = Object.keys(rows).sort((a, b) => Number(a) - Number(b));

  const totalPrice = tickets
    .filter(t => selected.includes(t.id))
    .reduce((sum, t) => sum + t.price, 0);

  return (
    <div className="max-w-4xl mx-auto px-6 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-1">{event.name}</h1>
      <p className="text-[#dc3558] font-medium text-sm">{event.performer?.name}</p>
      <p className="text-gray-500 text-sm">{event.venue?.name}</p>
      <p className="text-gray-400 text-xs mt-1 mb-6">
        {new Date(event.eventDate).toLocaleDateString('en-US', {
          weekday: 'long', year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'
        })}
      </p>

      <div className="bg-gray-100 text-center py-2 rounded-t-2xl mb-6 text-xs text-gray-500 tracking-[0.3em] font-medium border border-gray-200">
        STAGE
      </div>

      <div className="flex flex-col items-center gap-[6px] mb-6 bg-gray-50 py-6 px-4 rounded-lg">
        {sortedRowNames.map(rowName => (
          <div key={rowName} className="flex items-center gap-[6px]">
            <span className="w-6 text-[10px] text-gray-400 text-right mr-1">{rowName}</span>
            {rows[rowName].map(ticket => (
              <div
                key={ticket.id}
                onClick={() => toggleSeat(ticket)}
                title={`Row ${ticket.rowName} Seat ${ticket.seatNumber} — $${ticket.price}`}
                className={`w-7 h-7 rounded text-[10px] font-medium flex items-center justify-center transition-colors ${getSeatColor(ticket)}`}
              >
                {ticket.seatNumber}
              </div>
            ))}
          </div>
        ))}
      </div>

      <div className="flex gap-5 justify-center text-xs text-gray-500 mb-6">
        <span className="flex items-center gap-1.5">
          <span className="w-4 h-4 rounded border-2 border-[#1ea83c] inline-block" /> Available
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-4 h-4 rounded bg-[#1ea83c] inline-block" /> Selected
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-4 h-4 rounded bg-[#f5d76e] inline-block" /> Reserved
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-4 h-4 rounded bg-gray-300 inline-block" /> Sold
        </span>
      </div>

      {selected.length > 0 && (
        <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 shadow-lg px-6 py-4">
          <div className="max-w-4xl mx-auto flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">{selected.length} Ticket(s)</p>
              <p className="text-xl font-bold text-[#dc3558]">${totalPrice.toFixed(2)}</p>
            </div>
            <button
              onClick={handleReserve}
              disabled={booking}
              className="px-8 py-2.5 bg-[#dc3558] text-white rounded font-medium hover:bg-[#c22d4e] disabled:opacity-50 text-sm"
            >
              {booking ? 'Reserving...' : 'Book Now'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}