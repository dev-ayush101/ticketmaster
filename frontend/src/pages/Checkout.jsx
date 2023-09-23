import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import api from '../api';

export default function Checkout() {
  const { bookingId } = useParams();
  const navigate = useNavigate();
  const [booking, setBooking] = useState(null);
  const [timeLeft, setTimeLeft] = useState(600);
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

  if (!booking) return <p className="p-8 text-gray-500">Loading...</p>;

  return (
    <div className="max-w-md mx-auto px-6 py-8">
      <h1 className="text-xl font-bold text-gray-900 mb-4">Complete Your Booking</h1>

      {/* Timer */}
      <div className="bg-[#fff8e1] border border-[#ffe082] rounded px-4 py-3 mb-6 flex items-center gap-3">
        <span className="text-2xl">⏱️</span>
        <div>
          <p className={`text-xl font-mono font-bold ${timeLeft < 60 ? 'text-[#dc3558]' : 'text-gray-800'}`}>
            {String(mins).padStart(2, '0')}:{String(secs).padStart(2, '0')}
          </p>
          <p className="text-xs text-gray-500">Time remaining to complete payment</p>
        </div>
      </div>

      {/* Booking Summary */}
      <div className="border border-gray-200 rounded-lg p-4 mb-6">
        <h2 className="font-semibold text-sm text-gray-700 mb-3">Order Summary</h2>
        {booking.tickets?.map(t => (
          <div key={t.id} className="flex justify-between text-sm text-gray-600 mb-1.5">
            <span>Row {t.rowName}, Seat {t.seatNumber}</span>
            <span className="font-medium">${t.price}</span>
          </div>
        ))}
        <div className="border-t border-gray-200 mt-3 pt-3 flex justify-between font-bold text-gray-900">
          <span>Total</span>
          <span>${booking.totalPrice}</span>
        </div>
      </div>

      {/* Mock Payment Form */}
      <div className="border border-gray-200 rounded-lg p-4 mb-6">
        <h2 className="font-semibold text-sm text-gray-700 mb-3">Payment Details</h2>
        <input placeholder="Card Number" className="w-full mb-3 px-3 py-2 rounded border border-gray-300 text-sm focus:outline-none focus:border-[#dc3558]" />
        <div className="flex gap-3">
          <input placeholder="MM/YY" className="flex-1 px-3 py-2 rounded border border-gray-300 text-sm focus:outline-none focus:border-[#dc3558]" />
          <input placeholder="CVV" className="flex-1 px-3 py-2 rounded border border-gray-300 text-sm focus:outline-none focus:border-[#dc3558]" />
        </div>
      </div>

      <button
        onClick={handleConfirm}
        disabled={confirming}
        className="w-full py-3 bg-[#dc3558] text-white rounded font-semibold hover:bg-[#c22d4e] disabled:opacity-50 text-sm"
      >
        {confirming ? 'Confirming...' : `Pay $${booking.totalPrice}`}
      </button>
    </div>
  );
}