import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import api from '../api';

export default function Checkout() {
  const { bookingId } = useParams();
  const navigate = useNavigate();
  const [booking, setBooking] = useState(null);
  const [timeLeft, setTimeLeft] = useState(600); // 10 min
  const [confirming, setConfirming] = useState(false);

  useEffect(() => {
    api.get(`/bookings/${bookingId}`).then(res => setBooking(res.data));
  }, [bookingId]);

  useEffect(() => {
    if (timeLeft <= 0) {
      toast.error('Reservation expired');
      navigate('/');
      return;
    }
    const timer = setInterval(() => setTimeLeft(t => t - 1), 1000);
    return () => clearInterval(timer);
  }, [timeLeft, navigate]);

  const handleConfirm = async () => {
    setConfirming(true);
    try {
      await api.post(`/bookings/${bookingId}/confirm?userEmail=ayush@test.com`);
      toast.success('Booking confirmed!');
      navigate('/');
    } catch (err) {
      toast.error(err.response?.data?.error || 'Confirmation failed');
      setConfirming(false);
    }
  };

  const mins = Math.floor(timeLeft / 60);
  const secs = timeLeft % 60;

  if (!booking) return <p className="p-8 text-gray-400">Loading...</p>;

  return (
    <div className="max-w-lg mx-auto px-6 py-8">
      <h1 className="text-2xl font-bold mb-4">Checkout</h1>

      {/* Timer */}
      <div className={`text-center text-3xl font-mono mb-6 ${timeLeft < 60 ? 'text-red-400' : 'text-purple-400'}`}>
        {String(mins).padStart(2, '0')}:{String(secs).padStart(2, '0')}
      </div>
      <p className="text-center text-sm text-gray-500 mb-6">Complete your purchase before the timer runs out</p>

      {/* Booking Summary */}
      <div className="bg-gray-900 border border-gray-800 rounded-lg p-4 mb-6">
        <h2 className="font-semibold mb-3">Booking Summary</h2>
        {booking.tickets?.map(t => (
          <div key={t.id} className="flex justify-between text-sm text-gray-400 mb-1">
            <span>Row {t.rowName}, Seat {t.seatNumber}</span>
            <span>${t.price}</span>
          </div>
        ))}
        <div className="border-t border-gray-700 mt-3 pt-3 flex justify-between font-bold">
          <span>Total</span>
          <span>${booking.totalPrice}</span>
        </div>
      </div>

      {/* Mock Payment Form */}
      <div className="bg-gray-900 border border-gray-800 rounded-lg p-4 mb-6">
        <h2 className="font-semibold mb-3">Payment Details</h2>
        <input placeholder="Card Number" className="w-full mb-3 px-3 py-2 rounded bg-gray-800 border border-gray-700 text-sm" />
        <div className="flex gap-3">
          <input placeholder="MM/YY" className="flex-1 px-3 py-2 rounded bg-gray-800 border border-gray-700 text-sm" />
          <input placeholder="CVV" className="flex-1 px-3 py-2 rounded bg-gray-800 border border-gray-700 text-sm" />
        </div>
      </div>

      <button
        onClick={handleConfirm}
        disabled={confirming}
        className="w-full py-3 bg-purple-600 rounded-lg hover:bg-purple-700 font-semibold disabled:opacity-50"
      >
        {confirming ? 'Confirming...' : `Pay $${booking.totalPrice}`}
      </button>
    </div>
  );
}