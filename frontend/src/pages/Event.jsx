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

  useEffect(() => {
    api.get(`/events/${eventId}`).then(res => {
      setEvent(res.data.event);
      setTickets(res.data.tickets);
      setLoading(false);
    });
  }, [eventId]);

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
        userEmail: 'ayush@test.com'
      });
      toast.success('Seats reserved!');
      navigate(`/checkout/${res.data.id}`);
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to reserve');
      setBooking(false);
    }
  };

  const getSeatColor = (ticket) => {
    if (selected.includes(ticket.id)) return 'bg-purple-500';
    if (ticket.status === 'BOOKED') return 'bg-red-500';
    if (ticket.status === 'RESERVED') return 'bg-yellow-500';
    return 'bg-green-500 hover:bg-green-400 cursor-pointer';
  };

  if (loading) return <p className="p-8 text-gray-400">Loading...</p>;

  // group tickets by row
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
      <h1 className="text-2xl font-bold mb-1">{event.name}</h1>
      <p className="text-gray-400 mb-1">{event.performer?.name} · {event.venue?.name}</p>
      <p className="text-gray-500 text-sm mb-6">
        {new Date(event.eventDate).toLocaleDateString('en-US', {
          weekday: 'long', year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'
        })}
      </p>

      {/* Stage */}
      <div className="bg-gray-800 text-center py-2 rounded mb-6 text-sm text-gray-400 tracking-widest">
        STAGE
      </div>

      {/* Seat Map */}
      <div className="flex flex-col items-center gap-2 mb-6">
        {sortedRowNames.map(rowName => (
          <div key={rowName} className="flex items-center gap-1">
            <span className="w-8 text-xs text-gray-500 text-right mr-2">R{rowName}</span>
            {rows[rowName].map(ticket => (
              <div
                key={ticket.id}
                onClick={() => toggleSeat(ticket)}
                title={`Row ${ticket.rowName} Seat ${ticket.seatNumber} — $${ticket.price}`}
                className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-medium ${getSeatColor(ticket)}`}
              >
                {ticket.seatNumber}
              </div>
            ))}
          </div>
        ))}
      </div>

      {/* Legend */}
      <div className="flex gap-4 justify-center text-xs text-gray-400 mb-6">
        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded-full bg-green-500 inline-block" /> Available</span>
        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded-full bg-purple-500 inline-block" /> Selected</span>
        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded-full bg-yellow-500 inline-block" /> Reserved</span>
        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded-full bg-red-500 inline-block" /> Booked</span>
      </div>

      {/* Book */}
      {selected.length > 0 && (
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-4 flex items-center justify-between">
          <div>
            <p className="text-sm text-gray-400">{selected.length} seat(s) selected</p>
            <p className="text-lg font-bold">${totalPrice.toFixed(2)}</p>
          </div>
          <button
            onClick={handleReserve}
            disabled={booking}
            className="px-6 py-2 bg-purple-600 rounded-lg hover:bg-purple-700 disabled:opacity-50"
          >
            {booking ? 'Reserving...' : 'Reserve & Checkout'}
          </button>
        </div>
      )}
    </div>
  );
}