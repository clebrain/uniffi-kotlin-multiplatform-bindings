/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use std::collections::HashMap;
use std::sync::atomic::{AtomicBool, AtomicI32, Ordering};
use std::sync::{Arc, Mutex, RwLock};
use std::time::SystemTime;

use once_cell::sync::Lazy;

static NUM_ALIVE: Lazy<RwLock<u64>> = Lazy::new(|| RwLock::new(0));

#[derive(Debug, thiserror::Error)]
pub enum CoverallError {
    #[error("The coverall has too many holes")]
    TooManyHoles,
}

/// This error doesn't appear in the interface, instead
/// we rely on an `Into<CoverallError>` impl to surface it to consumers.
#[derive(Debug, thiserror::Error)]
pub enum InternalCoverallError {
    #[error("The coverall has an excess of holes")]
    ExcessiveHoles,
}

impl From<InternalCoverallError> for CoverallError {
    fn from(err: InternalCoverallError) -> CoverallError {
        match err {
            InternalCoverallError::ExcessiveHoles => CoverallError::TooManyHoles,
        }
    }
}

#[derive(Debug, thiserror::Error)]
pub enum ComplexError {
    #[error("OsError: {code} ({extended_code})")]
    OsError { code: i16, extended_code: i16 },
    #[error("PermissionDenied: {reason}")]
    PermissionDenied { reason: String },
}

#[derive(Debug, Clone)]
pub struct SimpleDict {
    text: String,
    maybe_text: Option<String>,
    a_bool: bool,
    maybe_a_bool: Option<bool>,
    unsigned8: u8,
    maybe_unsigned8: Option<u8>,
    unsigned16: u16,
    maybe_unsigned16: Option<u16>,
    unsigned64: u64,
    maybe_unsigned64: Option<u64>,
    signed8: i8,
    maybe_signed8: Option<i8>,
    signed64: i64,
    maybe_signed64: Option<i64>,
    float32: f32,
    maybe_float32: Option<f32>,
    float64: f64,
    maybe_float64: Option<f64>,
    byte_array: Vec<u8>,
    maybe_byte_array: Option<Vec<u8>>,
    coveralls: Option<Arc<Coveralls>>,
}

#[derive(Debug, Clone)]
pub struct DictWithDefaults {
    name: String,
    category: Option<String>,
    integer: u64,
}

#[derive(Debug, Clone)]
pub enum MaybeSimpleDict {
    Yeah { d: SimpleDict },
    Nah,
}

fn create_some_dict() -> SimpleDict {
    SimpleDict {
        text: "text".to_string(),
        maybe_text: Some("maybe_text".to_string()),
        a_bool: true,
        maybe_a_bool: Some(false),
        unsigned8: 1,
        maybe_unsigned8: Some(2),
        unsigned16: 3,
        maybe_unsigned16: Some(4),
        unsigned64: u64::MAX,
        maybe_unsigned64: Some(u64::MIN),
        signed8: 8,
        maybe_signed8: Some(0),
        signed64: i64::MAX,
        maybe_signed64: Some(0),
        float32: 1.2345,
        maybe_float32: Some(22.0 / 7.0),
        float64: 0.0,
        maybe_float64: Some(1.0),
        byte_array: vec![5, 4, 3, 2, 1],
        maybe_byte_array: Some(vec![10, 8, 6, 4, 2]),
        coveralls: Some(Arc::new(Coveralls::new("some_dict".to_string()))),
    }
}

fn create_none_dict() -> SimpleDict {
    SimpleDict {
        text: "text".to_string(),
        maybe_text: None,
        a_bool: true,
        maybe_a_bool: None,
        unsigned8: 1,
        maybe_unsigned8: None,
        unsigned16: 3,
        maybe_unsigned16: None,
        unsigned64: u64::MAX,
        maybe_unsigned64: None,
        signed8: 8,
        maybe_signed8: None,
        signed64: i64::MAX,
        maybe_signed64: None,
        float32: 1.2345,
        maybe_float32: None,
        float64: 0.0,
        maybe_float64: None,
        byte_array: vec![5, 4, 3, 2, 1],
        maybe_byte_array: None,
        coveralls: None,
    }
}

fn get_num_alive() -> u64 {
    *NUM_ALIVE.read().unwrap()
}

type Result<T, E = CoverallError> = std::result::Result<T, E>;
type ComplexResult<T, E = ComplexError> = std::result::Result<T, E>;

fn println(text: String) -> Result<()> {
    println!("coveralls println: {text}");
    Ok(())
}

#[derive(Debug)]
pub struct Coveralls {
    name: String,
    // A reference to another Coveralls. Currently will be only a reference
    // to `self`, so will create a circular reference.
    other: Mutex<Option<Arc<Self>>>,
    // Repairs we've made to this coverall.
    repairs: Mutex<Vec<Repair>>,
}

impl Coveralls {
    fn new(name: String) -> Self {
        *NUM_ALIVE.write().unwrap() += 1;
        Self {
            name,
            other: Mutex::new(None),
            repairs: Mutex::new(Vec::new()),
        }
    }

    fn fallible_new(name: String, should_fail: bool) -> Result<Self> {
        if should_fail {
            Err(CoverallError::TooManyHoles)
        } else {
            Ok(Self::new(name))
        }
    }

    fn fallible_panic(&self, message: String) -> Result<()> {
        panic!("{message}");
    }

    fn get_name(&self) -> String {
        self.name.clone()
    }

    fn panicing_new(message: String) -> Self {
        panic!("{message}");
    }

    fn maybe_throw(&self, should_throw: bool) -> Result<bool> {
        if should_throw {
            Err(CoverallError::TooManyHoles)
        } else {
            Ok(true)
        }
    }

    fn maybe_throw_into(&self, should_throw: bool) -> Result<bool, InternalCoverallError> {
        if should_throw {
            Err(InternalCoverallError::ExcessiveHoles)
        } else {
            Ok(true)
        }
    }

    fn maybe_throw_complex(&self, input: i8) -> ComplexResult<bool> {
        match input {
            0 => Ok(true),
            1 => Err(ComplexError::OsError {
                code: 10,
                extended_code: 20,
            }),
            2 => Err(ComplexError::PermissionDenied {
                reason: "Forbidden".to_owned(),
            }),
            _ => panic!("Invalid input"),
        }
    }

    fn panic(&self, message: String) {
        panic!("{message}");
    }

    fn strong_count(self: Arc<Self>) -> u64 {
        Arc::strong_count(&self) as u64
    }

    fn take_other(&self, other: Option<Arc<Self>>) {
        *self.other.lock().unwrap() = other.map(|arc| Arc::clone(&arc))
    }

    fn get_other(&self) -> Option<Arc<Self>> {
        (*self.other.lock().unwrap()).as_ref().map(Arc::clone)
    }

    fn take_other_fallible(self: Arc<Self>) -> Result<()> {
        Err(CoverallError::TooManyHoles)
    }

    fn take_other_panic(self: Arc<Self>, message: String) {
        panic!("{message}");
    }

    fn clone_me(&self) -> Arc<Self> {
        let other = self.other.lock().unwrap();
        let new_other = Mutex::new(other.clone());
        *NUM_ALIVE.write().unwrap() += 1;
        Arc::new(Self {
            name: self.name.clone(),
            other: new_other,
            repairs: Mutex::new(Vec::new()),
        })
    }

    fn get_status(&self, status: String) -> String {
        format!("status: {status}")
    }

    fn get_dict(&self, key: String, value: u64) -> HashMap<String, u64> {
        let mut map = HashMap::new();
        map.insert(key, value);
        map
    }

    fn get_dict2(&self, key: String, value: u64) -> HashMap<String, u64> {
        let mut map = HashMap::new();
        map.insert(key, value);
        map
    }

    fn get_dict3(&self, key: u32, value: u64) -> HashMap<u32, u64> {
        let mut map = HashMap::new();
        map.insert(key, value);
        map
    }

    fn add_patch(&self, patch: Arc<Patch>) {
        let repair = Repair {
            when: SystemTime::now(),
            patch,
        };
        let mut repairs = self.repairs.lock().unwrap();
        repairs.push(repair);
    }

    fn add_repair(&self, repair: Repair) {
        let mut repairs = self.repairs.lock().unwrap();
        repairs.push(repair);
    }

    fn get_repairs(&self) -> Vec<Repair> {
        let repairs = self.repairs.lock().unwrap();
        repairs.clone()
    }
}

impl Drop for Coveralls {
    fn drop(&mut self) {
        *NUM_ALIVE.write().unwrap() -= 1;
    }
}

#[derive(Debug, Clone)]
pub struct Repair {
    when: SystemTime,
    patch: Arc<Patch>,
}

#[derive(Debug, Clone, Copy)]
pub enum Color {
    Red,
    Blue,
    Green,
}

#[derive(Debug, Clone)]
struct Patch {
    color: Color,
}

impl Patch {
    fn new(color: Color) -> Self {
        Self { color }
    }

    fn get_color(&self) -> Color {
        self.color
    }
}

// This is a small implementation of a counter that allows waiting on one thread,
// and counting on another thread. We use it to test that the UniFFI generated scaffolding
// doesn't introduce unexpected locking behaviour between threads.
struct ThreadsafeCounter {
    is_busy: AtomicBool,
    count: AtomicI32,
}

impl ThreadsafeCounter {
    fn new() -> Self {
        Self {
            is_busy: AtomicBool::new(false),
            count: AtomicI32::new(0),
        }
    }

    fn busy_wait(&self, ms: i32) {
        self.is_busy.store(true, Ordering::SeqCst);
        // Pretend to do some work in a blocking fashion.
        std::thread::sleep(std::time::Duration::from_millis(ms as u64));
        self.is_busy.store(false, Ordering::SeqCst);
    }

    fn increment_if_busy(&self) -> i32 {
        if self.is_busy.load(Ordering::SeqCst) {
            self.count.fetch_add(1, Ordering::SeqCst) + 1
        } else {
            self.count.load(Ordering::SeqCst)
        }
    }
}

uniffi::include_scaffolding!("coverall");
uniffi_reexport_scaffolding!();
