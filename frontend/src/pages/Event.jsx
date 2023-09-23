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
    if (selected.includes(ticket.id)) return 'bg-[#1ea83c] text-white';
    if (ticket.status === 'BOOKED') return 'bg-gray-300 text-gray-400 cursor-not-allowed';
    if (ticket.status === 'RESERVED') return 'bg-[#f5d76e] text-gray-700 cursor-not-allowed';
    return 'bg-white border-2 border-[#1ea83c] text-[#1ea83c] hover:bg-[#e8f5e9] cursor-pointer';
  };

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

      {/* Stage */}
      <div className="bg-gray-100 text-center py-2 rounded-t-2xl mb-6 text-xs text-gray-500 tracking-[0.3em] font-medium border border-gray-200">
        STAGE
      </div>

      {/* Seat Map */}
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

      {/* Legend */}
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

      {/* Book */}
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